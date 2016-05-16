(ns matchbox.core
  (:refer-clojure
    :exclude
    [get-in set! reset! conj! swap! -swap! dissoc! deref parents key take take-last])
  #?(:clj
     (:import
       [com.firebase.client
        AuthData
        ChildEventListener
        Config
        DataSnapshot
        Firebase
        FirebaseError
        MutableData
        ServerValue
        Transaction
        Transaction$Handler
        ValueEventListener
        Firebase$CompletionListener
        Transaction$Result
        Logger$Level
        Firebase$AuthResultHandler
        Firebase$AuthStateListener]
       (java.util HashMap ArrayList)))
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [matchbox.utils :as utils]
    [matchbox.registry :refer [register-listener register-auth-listener disable-auth-listener!]]
    ;[matchbox.promise :as prom]
    [promesa.core :as prom]
    [matchbox.protocols :as proto]
    #?(:cljs cljsjs.firebase)))

;; Matchbox Public API Protocol
(defprotocol Matchbox
  "Matchbox Public API"

  (get-in
    [_ korks]
    "Obtain child Reference or DataSnapshot from base by following korks.")

  (deref
    [_ callback]
    [_ state callback]
    "Deref a Reference, Promise or DataSnapshot.")

  (deref-list
    [_]
    [_ callback]
    [_ state callback]
    "Deref a Reference, Promise or DataSnapshot and return a list of values.")

  (reset-priority!
    [_ priority]
    [_ priority callback]
    "Set priority value on Reference, Promise or DataSnapshot.")

  (reset!
    [_ val]
    [_ val callback]
    "Reset value on Reference, Promise or DataSnapshot.")

  (reset-with-priority!
    [_ val priority]
    [_ val priority callback]
    "Set priority value on Reference, Promise or DataSnapshot.")

  (-swap!
    [_ fn args]
    "Swap the value on a Reference, Promise or DataSnapshot.")

  (merge!
    [_ val]
    [_ val callback]
    "Merge a value with a Reference, Promise or DataSnapshot.")

  (conj!
    [_ val]
    [_ val callback]
    "Conjoin a value to a Reference, Promise or DataSnapshot.")

  (dissoc!
    [_]
    [_ callback]
    "Dissoc a Reference, Promise or DataSnapshot.")

  (order-priority
    [_]
    "Resolve Reference, Promise or DataSnapshot and Order by priority.")

  (order-key
    [_]
    "Resolve Reference, Promise or DataSnapshot and Order by key.")

  (order-value
    [_]
    "Resolve Reference, Promise or DataSnapshot and Order by value.")

  (order-child
    [_ child]
    "Resolve Reference, Promise or DataSnapshot and Order by child.")

  (start-at
    [_ val]
    [_ val key]
    "Limit query to begin at 'value' (inclusive).")

  (end-at
    [_ val]
    [_ val key]
    "Limit query to end at 'value' (inclusive).")

  (equal-to
    [_ val]
    [_ val key]
    "Limit query to 'value' (inclusive).")


  ;; Listener API

  ;; Auth API

  )

;; constants

;; Distinct from nil/null in CLJS, useful for opting out of callbacks
(def undefined)

(def child-events
  [:child-added
   :child-changed
   :child-moved
   :child-removed])

(def all-events
  (conj child-events :value))

#?(:clj
(def logger-levels
  {:debug Logger$Level/DEBUG
   :info  Logger$Level/INFO
   :warn  Logger$Level/WARN
   :error Logger$Level/ERROR
   :none  Logger$Level/NONE}))

#_#?(:clj
(defn set-logger-level! [key]
  (assert (contains? logger-levels key) (format "Unknown logger level: `%s`" key))
  (.setLogLevel ^Config (Firebase/getDefaultConfig)
                (logger-levels key))))


(def SERVER_TIMESTAMP
  #?(:clj ServerValue/TIMESTAMP
     :cljs js/Firebase.ServerValue.TIMESTAMP))

;; helpers

(declare wrap-snapshot)
(declare hydrate)
(declare reset!)

(defn throw-fb-error [err & [msg]]
  (throw (ex-info (or msg "FirebaseError") {:err err})))

#?(:clj
    (defn- wrap-cb [cb]
      (reify Firebase$CompletionListener
        (^void onComplete [_ ^FirebaseError err ^Firebase ref]
          (if err (throw-fb-error err "Cancelled") (cb ref))))))

#?(:clj
    (defn- reify-value-listener [cb & [ds-wrapper]]
      (let [ds-wrapper (or ds-wrapper wrap-snapshot)]
        (reify ValueEventListener
          (^void onDataChange [_ ^DataSnapshot ds]
            (cb (ds-wrapper ds)))
          (^void onCancelled [_ ^FirebaseError err]
            (if err (throw-fb-error err "Cancelled") (cb ref)))))))

#?(:clj
    (defn- build-tx-handler [f args cb]
      (reify Transaction$Handler
        (^Transaction$Result doTransaction [_ ^MutableData d]
          (let [current (hydrate (.getValue d))]
            (reset! d (apply f current args))
            (Transaction/success d)))
        (^void onComplete [_ ^FirebaseError error, ^boolean committed, ^DataSnapshot d]
          (if (and cb (not error) committed)
            (cb (hydrate (.getValue d))))))))

#?(:clj
    (defn reify-child-listener [{:keys [added changed moved removed]}]
      (reify ChildEventListener
        (^void onChildAdded [_ ^DataSnapshot d ^String _]
          (if added (added (wrap-snapshot d))))
        (^void onChildChanged [_ ^DataSnapshot d ^String _]
          (if changed (changed (wrap-snapshot d))))
        (^void onChildMoved [_ ^DataSnapshot d ^String _]
          (if moved (moved (wrap-snapshot d))))
        (^void onChildRemoved [_ ^DataSnapshot d]
          (if removed (removed (wrap-snapshot d))))
        (^void onCancelled [_ ^FirebaseError err]
          (throw-fb-error err "Cancelled")))))

#?(:clj
    (defn- strip-prefix [type]
      (-> type name (str/replace #"^.+\-" "") keyword)))

(defn- keywords->strings [x]
  (if (keyword? x) (str x) x))

(defn- hydrate-keywords [x]
  (if (and (string? x) (= \: (first x))) (keyword (subs x 1)) x))

#?(:clj
    (defn- hydrate* [x]
      (cond (instance? HashMap x) (recur (into {} x))
            (instance? ArrayList x) (recur (into [] x))
            (map? x) (zipmap (map keyword (keys x)) (vals x))
            :else (hydrate-keywords x))))

(defn hydrate [v]
  #?(:clj (walk/prewalk hydrate* v)
     :cljs (walk/postwalk
             hydrate-keywords
             (js->clj v :keywordize-keys true))))

(defn serialize [v]
  (->> (walk/stringify-keys v)
       (walk/postwalk keywords->strings)
       #?(:cljs clj->js)))

(defn- get-children [snapshot]
  (mapv proto/-val
        #?(:clj (.getChildren snapshot)
           ;; perhaps use js array if too much latency here for larger lists
           :cljs (let [kids (atom [])]
                   ;; must return falsey else iteration ends
                   (.forEach snapshot #(do (clojure.core/swap! kids conj %) undefined))
                   @kids))))

(defn --deref
  ([in callback]
   (--deref in (atom nil) callback))
  ([in state callback]
   #?(:clj  (proto/-val in (reify-value-listener callback #(hydrate (proto/-val %))))
      :cljs (proto/-val in #(comp callback (cljs.core/reset! state (hydrate (proto/-val %))))))))

(defn --deref-list
  ([ref callback]
   (--deref ref (atom nil) callback))
  ([ref state callback]
   (--deref ref state #(comp callback get-children))))

(defn -get-in
  [ref korks]
  (let [path (utils/korks->path korks)]
    (if-not (seq path) ref (proto/-child ref path))))

#?(:clj
    (extend-protocol Matchbox

      ;; Firebase Reference
      com.firebase.client.Firebase
      (get-in
        [ref korks]
        (-get-in ref korks))

      (deref
        ([ref callback]
         (--deref ref callback))
        ([ref state callback]
         (--deref ref state callback)))

      (deref-list
        ([ref callback]
         (--deref-list ref callback))
        ([ref state callback]
         (--deref-list ref state callback)))

      (reset!
        ([ref val]
         (reset! ref val identity))
        ([ref val callback]
         (.setValue ref (serialize val) (wrap-cb callback)))))

   :cljs
    (extend-protocol Matchbox

      ;; Firebase Reference
      js.Firebase
      (get-in
        [ref korks]
        (-get-in ref korks))

      (deref
        ([ref callback]
         (--deref ref callback))
        ([ref state callback]
         (--deref ref state callback)))

      (deref-list
        ([ref callback]
         (--deref-list ref callback))
        ([ref state callback]
         (--deref-list ref state callback)))

      (reset!
        ([ref val]
         (reset! ref val identity))
        ([ref val callback]
         (proto/-set ref (serialize val) callback)))

      (reset-with-priority!
        ([ref val priority]
         (reset-with-priority! ref val undefined))
        ([ref val priority callback]
         (proto/-setwithpriority ref (serialize val) priority callback)))

      (-swap!
        ([ref f args]
         (let [[callback args] (utils/extract :callback args)]
           (let [f' #(-> % hydrate ((fn [x] (apply f x args))) serialize)]
             (.transaction ref f' callback)))))

      (merge!
        ([ref val]
         (merge! ref val undefined))
        ([ref val callback]
         (.update ref (serialize val) callback)))

      (conj!
        ([ref val]
         (conj! ref val undefined))
        ([ref val callback]
         (let [k (atom nil)]
           (clojure.core/reset! k (proto/-key (.push ref (serialize val) callback)))
           @k)))

      (dissoc!
        ([ref callback]
         (.remove ref (or callback undefined))))

      (order-priority [ref] (proto/-orderbypriority ref))

      (order-key [ref] (proto/-orderbykey ref))

      (order-value [ref] (proto/-orderbyvalue ref))

      (order-child [ref child] (proto/-orderbychild ref child))

      (start-at
        ([ref val]
         (start-at ref val))
        ([ref val key]
         (start-at ref val key)))

      (end-at
        ([ref val]
         (end-at ref val))
        ([ref val key]
         (end-at ref val key)))

      (equal-to
        ([ref val]
         (equal-to ref val))
        ([ref val key]
         (equal-to ref val key)))

      ;; Firebase DataSnapshot
      object
      (get-in
        [ref korks]
        (get-in ref korks))

      (deref
        ([dat callback]
         (--deref dat callback))
        ([dat state callback]
         (--deref dat state callback)))

      (deref-list
        ([dat]
         (get-children dat))
        ([dat callback]
         (--deref-list dat callback))
        ([dat state callback]
         (--deref-list dat state callback)))

      (reset!
        ([dat val callback]
         (reset! (proto/-ref dat) val callback)))

      (-swap!
        ([dat fn args]
         (-swap! (proto/-ref dat) fn args)))

      (merge!
        ([dat val callback]
         (merge! (proto/-ref dat) val callback)))

      (conj!
        ([dat val callback]
         (conj! (proto/-ref dat) val callback)))

      (dissoc!
        ([dat]
         (dissoc! (proto/-ref dat) undefined))
        ([dat callback]
         (dissoc! (proto/-ref dat) callback)))

      (order-priority [dat] (order-priority (proto/-ref dat)))

      (order-key [dat] (order-key (proto/-ref dat)))

      (order-value [dat] (order-value (proto/-ref dat)))

      (order-child [dat child] (order-child (proto/-ref dat) child))

      (start-at
        ([dat val]
         (start-at (proto/-ref dat) val))
        ([dat val key]
         (start-at (proto/-ref dat) val key)))

      (end-at
        ([dat val]
         (end-at (proto/-ref dat) val))
        ([dat val key]
         (end-at (proto/-ref dat) val key)))

      (equal-to
        ([dat val]
         (equal-to (proto/-ref dat) val))
        ([dat val key]
         (equal-to (proto/-ref dat) val key)))))

(defn swap! [in fn & args]
  (-swap! in fn args))

(defn key
  "Last segment in reference or snapshot path"
  [in]
  (proto/-key in))

(defn value
  "Data stored within snapshot"
  [in]
  (hydrate (proto/-val in)))

(defn- wrap-snapshot [snapshot]
  [(key snapshot) (value snapshot)])

;; API

(defn connect
  "Create a reference for firebase"
  ([url]
   #?(:clj (Firebase. url)
      :cljs (js/Firebase. url)))
  ([url korks]
   (get-in (connect url) korks)))

(defn parent
  "Immediate ancestor of reference, if any"
  [ref]
  (proto/-parent ref))

(defn parents
  "Probably don't need this. Or maybe we want more zipper nav (siblings, in-order, etc)"
  [ref]
  (take-while identity (iterate parent (parent ref))))

;(defn deref [ref cb]
;  #?(:clj (.addListenerForSingleValueEvent ref (reify-value-listener cb value))
;     :cljs (.once ref "value" (comp cb value))))

;(defn deref-list [ref cb]
;  #?(:clj (.addListenerForSingleValueEvent ref (reify-value-listener cb get-children))
;     :cljs (.once ref "value" (comp cb #(get-children %)))))

;(defn reset! [ref val & [cb]]
;  #?(:clj
;      (if-not cb
;        (.setValue ref (serialize val))
;        (.setValue ref (serialize val) (wrap-cb cb)))
;     :cljs (.set ref (serialize val) (if cb
;                                       (fn [err]
;                                         (if err
;                                           (throw-fb-error err)
;                                           (cb ref)))
;                                       undefined))))

;(defn reset-with-priority! [ref val priority & [cb]]
;  #?(:clj (if-not cb
;            (.setValue ref (serialize val) priority)
;            (.setValue ref (serialize val) priority (wrap-cb cb)))
;     :cljs (.setWithPriority ref (serialize val) priority
;                             (if cb
;                               (fn [err]
;                                 (if err
;                                   (throw-fb-error err)
;                                   (cb ref)))
;                               undefined))))

;(defn merge! [ref val & [cb]]
;  #?(:clj
;      (if-not cb
;        (.updateChildren ref (serialize val))
;        (.updateChildren ref (serialize val) (wrap-cb cb)))
;     :cljs
;     (.update ref (serialize val) (if cb
;                                    (fn [err]
;                                      (if err
;                                        (throw-fb-error err)
;                                        (cb ref)))
;                                    undefined))))

;(defn conj! [ref val & [cb]]
;  #?(:clj (let [r (.push ref)]
;            (reset! r val cb)
;            (key r))
;     :cljs (let [k (atom nil)]
;             (clojure.core/reset!
;               k
;               (key
;                 (.push ref (serialize val)
;                        (if cb
;                          (fn [err]
;                            (if err
;                              (throw-fb-error err)
;                              (cb (get-in ref @k))))
;                          undefined))))
;             @k)))

;(defn swap!
;  "Update value atomically, with local optimistic writes"
;  [ref f & args]
;  (let [[cb args] (utils/extract :callback args)]
;    #?(:clj
;        (.runTransaction ref (build-tx-handler f args cb) true)
;       :cljs
;       (let [f' #(-> % hydrate ((fn [x] (apply f x args))) serialize)]
;         (.transaction ref f' (if cb
;                                (fn [err commit? ds]
;                                  (if err
;                                    (throw-fb-error err)
;                                    (cb (value ds))))
;                                undefined))))))

;(defn dissoc! [ref & [cb]]
;  #?(:clj (if-not cb
;            (.removeValue ref)
;            (.removeValue ref (wrap-cb cb)))
;     :cljs (.remove ref (or cb undefined))))

(def remove! dissoc!)

(defn set-priority! [ref priority & [cb]]
  #?(:clj
      (if-not cb
        (.setPriority ref priority)
        (.setPriority ref priority (wrap-cb cb)))
     :cljs
     (.setPriority ref priority (or cb undefined))))

;;

(defn ref? [x] (instance? #?(:clj Firebase :cljs js/Firebase) x))

(defn- with-ds [ref-or-ds f & [cb]]
  (if (ref? ref-or-ds)
    (let [ref ref-or-ds]
      (assert cb "Callback required when called against reference")
      #?(:clj (.addListenerForSingleValueEvent ref (reify-value-listener cb f))
         :cljs (.once ref "value" (comp cb f))))
    (let [ds ref-or-ds
          v (f ds)]
      (if cb (cb v) v))))

(defn- -export [ds]
  (hydrate
    #?(:clj (.getValue ds true)
       :cljs (.exportVal ds))))

(defn- -priority [ds]
  (.getPriority ds))

(defn export [ref-or-ds & [cb]]
  (with-ds ref-or-ds -export cb))

(defn priority [ref-or-ds & [cb]]
  (with-ds ref-or-ds -priority cb))

(defn export-in [ref-or-ds korks & [cb]]
  (if (ref? ref-or-ds)
    (export (get-in ref-or-ds korks) cb)
    (export ref-or-ds (comp cb #(get-in % korks)))))

(defn priority-in [ref-or-ds korks & [cb]]
  (if (ref? ref-or-ds)
    (priority (get-in ref-or-ds korks) cb)
    (priority ref-or-ds (comp cb #(get-in % korks)))))

;;

(defn order-by-priority [ref]
  (.orderByPriority ref))

(defn order-by-key [ref]
  (.orderByKey ref))

(defn order-by-value [ref]
  (.orderByValue ref))

(defn order-by-child [ref key]
  (.orderByChild ref (name key)))

;;

;(defn start-at
;  "Limit query to start at `value` (inclusive). By default `value` is compared against
;   priorities, but reacts to the `order-by-*` scope. This also affects what types
;   `value can take on.
;
;   `key` is the child key to start at, and is supported only when ordering by priority."
;  [ref value & [key]]
;  (let [value (if (number? value) (double value) value)]
;    (if key
;      (.startAt ref value (name key))
;      (.startAt ref value))))

;(defn end-at
;  "Limit query to end at `value` (inclusive). By default `value` is compared against
;   priorities, but reacts to the `order-by-*` scope. This also affects what types
;   `value can take on.
;
;   `key` is the child key to end at, and is supported only when ordering by priority."
;  [ref value & [key]]
;  (let [value (if (number? value) (double value) value)]
;    (if key
;      (.endAt ref value (name key))
;      (.endAt ref value))))

;(defn equal-to
;  "Limit query to `value` (inclusive). By default `value` is compared against
;   priorities, but reacts to the `order-by-*` scope. This also affects what types
;   `value can take on.
;
;  `key` is the child key to compare on, and is supported only when ordering by priority."
;  [ref value & [key]]
;  (let [value (if (number? value) (double value) value)]
;    (if key
;      (.equalTo ref value (name key))
;      (.equalTo ref value))))

(defn take
  "Limit scope to the first `limit` items"
  [ref limit]
  (.limitToFirst ref limit))

(defn take-last
  "Limit scope to the last `limit` items"
  [ref limit]
  (.limitToLast ref limit))

;;

(defonce connected (atom true))

(defn disconnect! []
  #?(:cljs (.goOffline js/Firebase))
  (clojure.core/reset! connected false))

(defn reconnect! []
  #?(:cljs (.goOnline js/Firebase))
  (clojure.core/reset! connected true))

(defn connected?
  "Returns boolean around whether client is set to synchronise with server.
   Says nothing about actual connectivity."
  []
  @connected)

(defn on-disconnect [ref]
  #?(:cljs (.onDisconnect ref)))

(defn cancel [ref-disconnect & [cb]]
  #?(:cljs (.cancel ref-disconnect (or cb undefined))))

;; --------------------
;; auth

#?(:cljs
    (defn build-opts [session-only?]
      (if session-only?
        #js {:remember "sessionOnly"}
        undefined)))

(defn- ensure-kw-map
  "Coerce java.util.HashMap and friends to keywordized maps"
  [data]
  (walk/keywordize-keys (into {} data)))

(defn- auth-data->map [auth-data]
  #?(:cljs (hydrate auth-data)
     :clj (if auth-data
            {:uid           (.getUid auth-data)
             :provider      (keyword (.getProvider auth-data))
             :token         (.getToken auth-data)
             :expires       (.getExpires auth-data)
             :auth          (ensure-kw-map (.getAuth auth-data))
             :provider-data (ensure-kw-map (.getProviderData auth-data))})))

(defn wrap-auth-changed [cb]
  #?(:cljs
     (if cb
       (fn [err info]
         (cb err (js->clj info :keywordize-keys true)))
       identity)
     :clj
     (reify Firebase$AuthStateListener
       (^void onAuthStateChanged [_ ^AuthData auth-data]
         (if cb (cb nil (auth-data->map auth-data)))))))

(defn- wrap-auth-cb [cb]
  #?(:cljs
      (if cb
        (fn [err info]
          (cb err (js->clj info :keywordize-keys true)))
        identity)
     :clj
      (reify Firebase$AuthResultHandler
        (^void onAuthenticated [_ ^AuthData auth-data]
          (if cb (cb nil (auth-data->map auth-data))))
        (^void onAuthenticationError [_ ^FirebaseError err]
          (if cb (cb err nil))))))

(defn create-user
  "create-user creates a user in the Firebase built-in authentication server"
  [ref email password & [cb]]
  (.createUser ref
               #?@(:cljs [#js {:email email, :password password}]
                  :clj [email password])
               (wrap-auth-cb cb)))

(defn auth [ref email password & [cb session-only?]]
  (.authWithPassword ref
                     #?(:cljs #js {:email email, :password password})
                     #?@(:clj [email password])
                     (wrap-auth-cb cb)
                     #?(:cljs (build-opts session-only?))))

(defn auth-anon [ref & [cb session-only?]]
  (.authAnonymously ref
                    (wrap-auth-cb cb)
                    ;; Note: session-only? ignored on JVM
                    #?(:cljs (build-opts session-only?))))

(defn auth-custom
  "Authenticates a Firebase client using an authentication token or Firebase Secret."
  ([ref secret]
   (auth-custom ref secret nil))
  ([ref secret cb]
   (.authWithCustomToken ref secret (wrap-auth-cb cb)))
  #?(:cljs
      ([ref secret cb session-only?]
       (.authWithCustomToken ref
                             secret
                             (wrap-auth-cb cb)
                             (build-opts session-only?)))))

#?(:cljs
(defn auth-with-oauth-popup
  ([ref type]
    (auth-with-oauth-popup ref type undefined))
  ([ref type cb]
    (.authWithOAuthPopup ref type (wrap-auth-cb cb)))
  ([ref type cb options]
   (.authWithOAuthPopup ref type (wrap-auth-cb cb) (clj->js options)))))

#?(:cljs
   (defn auth-with-oauth-redirect
     ([ref type]
      (auth-with-oauth-redirect ref type undefined))
     ([ref type cb]
      (.authWithOAuthRedirect ref type (wrap-auth-cb cb)))
     ([ref type cb options]
      (.authWithOAuthRedirect ref type (wrap-auth-cb cb) (clj->js options)))))

#?(:cljs
   (defn auth-with-oauth-token
     ([ref type token-or-obj]
      (auth-with-oauth-token ref type token-or-obj undefined))
     ([ref type token-or-obj cb]
      (.authWithOAuthToken ref type token-or-obj (wrap-auth-cb cb)))
     ([ref type token-or-obj cb options]
      (.authWithOAuthToken ref type token-or-obj (wrap-auth-cb cb) (clj->js options)))))

(defn auth-info
  "Returns a map of uid, provider, token, expires - or nil if there is no session"
  [ref]
  (auth-data->map (.getAuth ref)))

;; onAuth and offAuth are not fully wrapped yet

(defn onAuth [ref cb]
  (register-auth-listener ref cb (wrap-auth-cb cb)))

(defn offAuth [ref cb]
  (disable-auth-listener! ref cb))

(defn unauth [ref]
  (.unauth ref))

;; nested variants

(defn deref-in [ref korks cb]
  (deref (get-in ref korks) cb))

(defn deref-list-in [ref korks cb]
  (deref-list (get-in ref korks) cb))

(defn reset-in! [ref korks val & [cb]]
  (reset! (get-in ref korks) val cb))

(defn reset-with-priority-in! [ref korks val priority & [cb]]
  (reset-with-priority! (get-in ref korks) val priority cb))

(defn merge-in! [ref korks val & [cb]]
  (merge! (get-in ref korks) val cb))

(defn conj-in! [ref korks val & [cb]]
  (conj! (get-in ref korks) val cb))

(defn swap-in! [ref korks f & args]
  (apply swap! (get-in ref korks) f args))

(defn dissoc-in! [ref korks & [cb]]
  (dissoc! (get-in ref korks) cb))

(def remove-in! dissoc-in!)

(defn set-priority-in! [ref korks priority & [cb]]
  (set-priority! (get-in ref korks) priority cb))

;; ------------------
;; subscriptions

(defn --listen-to [ref type cb render-fn]
  #?(:clj
      (let [listener (if-not (some #{type} child-events)
                       ;; subscribe
                       (.addValueEventListener ref (reify-value-listener cb render-fn))
                       (.addChildEventListener ref (reify-child-listener
                                                     (hash-map (strip-prefix type) cb))))]
        ;; build unsubsubscribe fn
        (fn [] (.removeEventListener ref listener)))
     :cljs
      (let [type (utils/kebab->underscore type)]
        (let [listener (comp cb render-fn)]
          ;; subscribe
          (.on ref type listener)
          ;; build unsubsubscribe fn
          (fn [] (.off ref type listener))))))

(defn- -listen-to [ref type cb & [render-fn]]
  (let [render-fn (or render-fn wrap-snapshot)]
    (assert (some #{type} all-events) (str "Unknown type: " type))
    (let [unsub! (--listen-to ref type cb render-fn)]
      (register-listener ref type unsub!)
      unsub!)))

(defn- -listen-children [ref cb]
  (let [cbs (->> child-events
                 (map (fn [type] #(vector type %)))
                 (map #(comp cb %)))
        ;; NOTE: JVM implementation could create a single listener
        unsubs (doall (map -listen-to (repeat ref) child-events cbs))]
    (fn []
      (doseq [unsub! unsubs]
        (unsub!)))))

(defn listen-to
  "Subscribe to notifications of given type
   Callback receives [<key> <value>] as only argument
   Returns an unsubscribe function"
  ([ref type cb] (-listen-to ref type cb))
  ([ref korks type cb] (-listen-to (get-in ref korks) type cb)))

(defn listen-list
  "Subscribe to updates containing full vector or children"
  ([ref cb] (-listen-to ref :value cb get-children))
  ([ref korks cb] (listen-list (get-in ref korks) cb)))

(defn listen-children
  "Subscribe to all children notifications on a reference.
   Callback receives [:event-type [<key> <value>]] as only argument
   Returns an unsubscribe function"
  ([ref cb] (-listen-children ref cb))
  ([ref korks cb] (-listen-children (get-in ref korks) cb)))
