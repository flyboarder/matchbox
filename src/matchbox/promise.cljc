(ns matchbox.promise
  (:refer-clojure :exclude [promise map deref reset! swap! conj! dissoc! take take-last])
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [promesa.core :as prom]
    [matchbox.core :as mbox]
    [matchbox.protocols :as proto]
    [matchbox.utils :as utils]))

;; this namespace should only be used with javascript (for now...)

(def promise prom/promise)

(def then prom/then)

(def catch prom/catch)

(def map prom/map)

(def chain prom/chain)

(def branch prom/branch)

#?(:cljs
    ;; Firebase Reference
    (extend-type js.Firebase

      prom/IPromise
      (-map
        [it cb]
        (.then (prom/promise it) #(cb %)))
      (-bind
        [it cb]
        (.then (prom/promise it) #(cb %)))
      (-catch
        [it cb]
        (.catch (prom/promise it) #(cb %)))))

#?(:cljs
    ;; Firebase Promise
    (extend-type prom/Promise

      proto/Firebase
      (-ref [p] (prom/then p -ref))

      (-key [p] (prom/then p -key))

      (-val
        ([p] (prom/then p -val))
        ([p callback] (prom/chain p -val callback)))

      (-child [p path] (prom/then p #(-child % path)))

      proto/FirebaseRef
      (-authcustomtoken
        ([p token]
         (-authcustomtoken p token matchbox.core/undefined))
        ([p token callback]
         (-authcustomtoken p token callback nil))
        ([p token callback opts]
         (prom/then p #(-authcustomtoken % token callback (clj->js opts)))))

      (-authanonymous
        ([p callback]
         (-authanonymous p callback nil))
        ([p callback opts]
         (prom/then p #(-authanonymous % callback (clj->js opts)))))

      (-authuserpass
        ([p cred]
         (-authuserpass p cred matchbox.core/undefined))
        ([p cred callback]
         (-authuserpass p cred callback nil))
        ([p cred callback opts]
         (prom/then p #(-authuserpass % cred callback opts))))

      (-authoauthpopup
        ([p provider]
         (-authoauthpopup p provider matchbox.core/undefined))
        ([p provider callback]
         (-authoauthpopup p provider callback nil))
        ([p provider callback opts]
         (prom/then p #(-authoauthpopup % provider callback opts))))

      (-authoauthredirect
        ([p provider]
         (-authoauthredirect p provider matchbox.core/undefined))
        ([p provider callback]
         (-authoauthredirect p provider callback nil))
        ([p provider callback opts]
         (prom/then p #(-authoauthredirect % provider callback opts))))

      (-authoauthtoken
        ([p provider cred]
         (-authoauthtoken p provider cred matchbox.core/undefined))
        ([p provider cred callback]
         (-authoauthtoken p provider cred callback nil))
        ([p provider cred callback opts]
         (prom/then p #(-authoauthtoken % provider cred callback opts))))

      (-getauth [p] (prom/then p -getauth))

      (-onauth  [p callback] (prom/then p #(-onauth % callback)))

      (-offauth [p callback] (prom/then p #(-offauth % callback)))

      (-unauth  [p] (prom/then p -unauth))

      (-parent  [p] (prom/then p -parent))

      (-root    [p] (prom/then p -root))

      (-tostr   [p] (prom/then p -tostr))

      (-set
        ([p value]
         (-set p value matchbox.core/undefined))
        ([p value callback]
         (prom/then p #(-set % value callback))))

      (-update
        ([p value]
         (-update p value matchbox.core/undefined))
        ([p value callback]
         (prom/then p #(-update % value callback))))

      (-remove
        ([p]
         (-remove p matchbox.core/undefined))
        ([p callback]
         (prom/then p #(-remove % callback))))

      (-push
        ([p value]
         (-push p value matchbox.core/undefined))
        ([p value callback]
         (prom/then p #(-push % value callback))))

      (-setwithpriority
        ([p value priority]
         (-setwithpriority p value priority matchbox.core/undefined))
        ([p value priority callback]
         (prom/then p #(-setwithpriority % value priority callback))))

      (-setpriority
        ([p priority]
         (-setpriority p priority matchbox.core/undefined))
        ([p priority callback]
         (prom/then p #(-setpriority % priority callback))))

      (-transaction
        ([p updatefn]
         (-transaction p updatefn matchbox.core/undefined))
        ([p updatefn callback]
         (-transaction p updatefn callback false))
        ([p updatefn callback applylocally]
         (prom/then p #(-transaction % updatefn callback applylocally))))

      (-createuser
        ([p cred]
         (-createuser p cred matchbox.core/undefined))
        ([p cred callback]
         (prom/then p #(-createuser % cred callback))))

      (-changeemail
        ([p cred]
         (-changeemail p cred matchbox.core/undefined))
        ([p cred callback]
         (prom/then p #(-changeemail % cred callback))))

      (-changepass
        ([p cred]
         (-changepass p cred matchbox.core/undefined))
        ([p cred callback]
         (prom/then p #(-changepass % cred callback))))

      (-removeuser
        ([p cred]
         (-removeuser p cred matchbox.core/undefined))
        ([p cred callback]
         (prom/then p #(-removeuser % cred callback))))

      (-resetpass
        ([p cred]
         (-resetpass p cred matchbox.core/undefined))
        ([p cred callback]
         (prom/then p #(-resetpass % cred callback))))

      (-goonline  [p] (prom/then p #(-goonline %)))

      (-gooffline [p] (prom/then p #(-gooffline %)))

      FirebaseQuery
      (-on
        ([p event callback]
         (-on p event callback matchbox.core/undefined))
        ([p event callback failure]
         (prom/then p #(-on % event callback failure))))

      (-off
        ([p event]
         (-off p event matchbox.core/undefined))
        ([p event callback]
         (prom/then p #(-off % event callback))))

      (-once
        ([p event]
         (-once p event matchbox.core/undefined))
        ([p event callback]
         (-once p event callback matchbox.core/undefined))
        ([p event callback failure]
         (prom/then p #(-once % event callback failure))))

      (-orderbychild    [p key] (prom/then p #(-orderbychild % key)))

      (-orderbykey      [p] (prom/then p #(-orderbykey %)))

      (-orderbyvalue    [p] (prom/then p #(-orderbyvalue %)))

      (-orderbypriority [p] (prom/then p #(-orderbypriority %)))

      (-startat
        ([p value]
         (-startat p value nil))
        ([p value key]
         (prom/then p #(-startat % value key))))

      (-endat
        ([p value]
         (-endat p value nil))
        ([p value key]
         (prom/then p #(-endat % value key))))

      (-equalto
        ([p value]
         (-equalto p value nil))
        ([p value key]
         (prom/then p #(-equalto % value key))))

      (-limitfirst [p limit] (prom/then p #(-limitfirst % limit)))

      (-limitlast  [p limit] (prom/then p #(-limitlast % limit)))

      FirebaseSnapshot
      (-exists      [p] (prom/then p -exists))

      (-foreach     [p callback] (prom/then p #(-foreach % callback)))

      (-haschild    [p path] (prom/then p #(-haschild % path)))

      (-haschildren [p] (prom/then p -haschildren))

      (-numchildren [p] (prom/then p -numchildren))

      ))


#?(:cljs
    (defn --deref
      ([in]
       (chain in proto/-val mbox/hydrate))
      ([in state]
       (--deref in state mbox/undefined))
      ([in state callback]
       (proto/-val in #(comp (cljs.core/reset! state (mbox/hydrate (proto/-val %)))
                              (callback %))))))
#?(:cljs
    (defn --deref-list
      ([ref]
       (proto/-once ref "value" mbox/get-children))
      ([ref state]
       (--deref ref state mbox/get-children))
      ([ref state callback]
       (--deref ref state #(callback (mbox/get-children %))))))

#?(:cljs
    (extend-protocol proto/Matchbox

      ;; Firebase Reference
      js.Firebase
      (get-in
        [ref korks]
        (mbox/get-in ref korks))

      (parent
        [ref]
        (proto/-parent ref))

      (-deref
        ([ref]
         (--deref ref))
        ([ref state]
         (--deref ref state))
        ([ref state callback]
         (--deref ref state callback)))

      (deref-list
        ([ref]
         (--deref-list ref))
        ([ref state]
         (--deref-list ref state))
        ([ref state callback]
         (--deref-list ref state callback)))

      (-reset!
        ([ref val]
         (mbox/reset! ref val mbox/undefined))
        ([ref val callback]
         (mbox/reset! ref val callback)))

      (-swap!
        [ref fn args]
        (apply (partial mbox/swap! ref fn) args))

      (-merge!
        ([ref val]
         (mbox/merge! ref val mbox/undefined))
        ([ref val callback]
         (mbox/merge! ref val callback)))

      (-conj!
        ([ref val]
         (mbox/conj! ref val mbox/undefined))
        ([ref val callback]
         (mbox/conj! ref val callback)))

      (-dissoc!
        ([ref]
         (mbox/dissoc! ref mbox/undefined))
        ([ref callback]
         (mbox/dissoc! ref callback)))

      (order-priority [ref] (mbox/order-by-priority ref))

      (order-key [ref] (mbox/order-by-key ref))

      (order-value [ref] (mbox/order-by-value ref))

      (order-child [ref child] (mbox/order-by-child ref child))

      (start-at
        ([ref val]
         (mbox/start-at ref val))
        ([ref val key]
         (mbox/start-at ref val key)))

      (end-at
        ([ref val]
         (mbox/end-at ref val))
        ([ref val key]
         (mbox/end-at ref val key)))

      (equal-to
        ([ref val]
         (mbox/equal-to ref val))
        ([ref val key]
         (mbox/equal-to ref val key)))

      ;; Firebase Promise
      prom/Promise
      (get-in
        [p korks]
        (then p #(get-in % korks)))

      (parent
        [p]
        (then p proto/-parent))

      (-deref
        ([p]
         (--deref p))
        ([p state]
         (--deref p state))
        ([p state callback]
         (--deref p state callback)))

      (deref-list
        ([p]
         (--deref-list p))
        ([p state]
         (--deref-list p state))
        ([p state callback]
         (--deref-list p state callback)))

      (-reset!
        ([p val]
         (then p #(mbox/reset! (proto/-ref %) val mbox/undefined)))
        ([p val callback]
         (then p #(mbox/reset! (proto/-ref %) val callback))))

      (-swap!
        ([p fn args]
         (then p #(swap! % fn args))))

      (-merge!
        ([p val]
         (then p #(mbox/merge! (proto/-ref %) val mbox/undefined)))
        ([p val callback]
         (then p #(mbox/merge! (proto/-ref %) val callback))))

      (-conj!
        ([p val]
         (then p #(mbox/conj! (proto/-ref %) val mbox/undefined)))
        ([p val callback]
         (then p #(mbox/conj! (proto/-ref %) val callback))))

      (-dissoc!
        ([p]
         (then p #(mbox/dissoc! (proto/-ref %) callback)))
        ([p callback]
         (then p #(mbox/dissoc! (proto/-ref %) callback))))

      (order-priority [p] (then p proto/order-priority))

      (order-key [p] (then p proto/order-key))

      (order-value [p] (then p proto/order-value))

      (order-child [p child] (then p #(proto/order-child % child)))

      (start-at
        ([p val]
         (then p #(proto/start-at % val)))
        ([p val key]
         (then p #(proto/start-at % val key))))

      (end-at
        ([p val]
         (then p #(proto/end-at % val)))
        ([p val key]
         (then p #(proto/end-at % val key))))

      (equal-to
        ([p val]
         (then p #(proto/equal-to % val)))
        ([p val key]
         (then p #(proto/equal-to % val key))))

      ;; Firebase DataSnapshot
      object
      (get-in
        [ref korks]
        (mbox/get-in ref korks))

      (parent
        [dat]
        (proto/-parent dat))

      (-deref
        ([dat]
         (-> dat proto/-val mbox/hydrate))
        ([dat state]
         (--deref dat state))
        ([dat state callback]
         (--deref dat state callback)))

      (deref-list
        ([dat]
         (mbox/get-children dat))
        ([dat state]
         (--deref-list dat state))
        ([dat state callback]
         (--deref-list dat state callback)))

      (-reset!
        ([dat val]
         (mbox/reset! (proto/-ref dat) val mbox/undefined))
        ([dat val callback]
         (mbox/reset! (proto/-ref dat) val callback)))

      (-swap!
        ([dat fn args]
         (swap! (proto/-ref dat) fn args)))

      (-merge!
        ([dat val]
         (mbox/merge! (proto/-ref dat) val mbox/undefined))
        ([dat val callback]
         (mbox/merge! (proto/-ref dat) val callback)))

      (-conj!
        ([dat val]
         (mbox/conj! (proto/-ref dat) val mbox/undefined))
        ([dat val callback]
         (mbox/conj! (proto/-ref dat) val callback)))

      (-dissoc!
        ([dat]
         (mbox/dissoc! (proto/-ref dat) mbox/undefined))
        ([dat callback]
         (mbox/dissoc! (proto/-ref dat) callback)))

      (order-priority [dat] (proto/order-priority (proto/-ref dat)))

      (order-key [dat] (proto/order-key (proto/-ref dat)))

      (order-value [dat] (proto/order-value (proto/-ref dat)))

      (order-child [dat child] (proto/order-child (proto/-ref dat) child))

      (start-at
        ([dat val]
         (mbox/start-at (proto/-ref dat) val))
        ([dat val key]
         (mbox/start-at (proto/-ref dat) val key)))

      (end-at
        ([dat val]
         (mbox/end-at (proto/-ref dat) val))
        ([dat val key]
         (mbox/end-at (proto/-ref dat) val key)))

      (equal-to
        ([dat val]
         (mbox/equal-to (proto/-ref dat) val))
        ([dat val key]
         (mbox/equal-to (proto/-ref dat) val key)))

))
