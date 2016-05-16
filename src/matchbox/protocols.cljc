(ns matchbox.protocols
  (:refer-clojure :exclude [-deref -reset! -swap! -conj! -dissoc! -key -val])
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [promesa.core :as prom]
    [matchbox.utils :as utils]
    #?(:cljs cljsjs.firebase)))

(def undefined)

;; Firebase Common Protocol
(defprotocol Firebase
  "Common firebase abstractions."

  (-ref
    [_]
    "Gets a Firebase reference to the location.")

  (-key
    [_]
    "Returns the last token in a Firebase location.")

  (-val
    [_]
    [_ callback]
    "Gets the JavaScript object representation of the Reference or DataSnapshot.")

  (-child
    [_ path]
    "Gets a Firebase Reference or DataSnapshot for the location at the specified relative path.")
)

#?(:clj
    (extend-protocol Firebase

      ;; Firebase Reference
      com.firebase.client.Firebase
      (-ref [ref] (.getRef ref))

      (-key [ref] (.getKey ref))

      (-val
        ([ref]
         (-val ref identity))
        ([ref callback]
         (.addListenerForSingleValueEvent ref callback)))

      (-child [ref path] (.child ref path))

      ;; Firebase DataSnapshot
      com.firebase.client.DataSnapshot
      (-ref [dat] (.getRef dat))

      (-key [dat] (.getKey dat))

      (-val
        ([dat]
         (-val ref identity))
        ([dat callback]
         (comp callback (.getValue dat))))

      (-child [dat path] (.child dat path)))

   :cljs
    (extend-protocol Firebase

      ;; Firebase Reference
      js.Firebase
      (-ref [ref] (.ref ref))

      (-key [ref] (.key ref))

      (-val
        ([ref] (prom/then (.once ref "value") #(.val %)))
        ([ref callback] (.once ref "value" callback)))

      (-child [ref path] (.child ref path))

      ;; Firebase DataSnapshot
      object
      (-ref [dat] (.ref dat))

      (-key [dat] (.key dat))

      (-val
        ([dat] (.val dat))
        ([dat callback] (comp callback (.val dat))))

      (-child [dat path] (.child dat path))))

;; Firebase Reference Protocol
(defprotocol FirebaseRef
  "A firebase reference abstraction."

  (-authcustomtoken
    [_ token]
    [_ token callback]
    [_ token callback opts]
    "Authenticates a Firebase client using an authentication token or Firebase Secret.")

  (-authanonymous
    [_ callback]
    [_ callback opts]
    "Authenticates a Firebase client using a new, temporary guest account.")

  (-authuserpass
    [_ cred]
    [_ cred callback]
    [_ cred callback opts]
    "Authenticates a Firebase client using an email / password combination.")

  (-authoauthpopup
    [_ provider]
    [_ provider callback]
    [_ provider callback opts]
    "Authenticates a Firebase client using a popup-based OAuth flow.")

  (-authoauthredirect
    [_ provider]
    [_ provider callback]
    [_ provider callback opts]
    "Authenticates a Firebase client using a redirect-based OAuth flow.")

  (-authoauthtoken
    [_ provider cred]
    [_ provider cred callback]
    [_ provider cred callback opts]
    "Authenticates a Firebase client using OAuth access tokens or credentials.")

  (-getauth
    [_]
    "Synchronously retrieves the current authentication state of the client.")

  (-onauth
    [_ callback]
    "Listens for changes to the client's authentication state.")

  (-offauth
    [_ callback]
    "Detaches a callback previously attached with onauth.")

  (-unauth
    [_]
    "Unauthenticates a Firebase client.")

  (-parent
    [_]
    "Gets a Firebase reference to the parent location.")

  (-root
    [_]
    "Gets a Firebase reference to the root of the Firebase.")

  (-tostr
    [_]
    "Gets the absolute URL corresponding to this Firebase reference's location.")

  (-set
    [_ value]
    [_ value callback]
    "Writes data to this Firebase location.")

  (-update
    [_ value]
    [_ value callback]
    "Writes the enumerated children to this Firebase location. Passing null to update() will remove the value at the specified location.")

  (-remove
    [_]
    [_ callback]
    "Removes the data at this Firebase location.")

  (-push
    [_ value]
    [_ value callback]
    "Generates a new child location using a unique key and returns a Firebase reference to it.")

  (-setwithpriority
    [_ value priority]
    [_ value priority callback]
    "Writes data to this Firebase location. Like set but also specifies the priority for that data.")

  (-setpriority
    [_ priority]
    [_ priority callback]
    "Sets a priority for the data at this Firebase location.")

  (-transaction
    [_ updatefn]
    [_ updatefn callback]
    [_ updatefn callback applylocally]
    "Atomically modifies the data at this location.")

  (-createuser
    [_ cred]
    [_ cred callback]
    "Creates a new user account using an email / password combination.")

  (-changeemail
    [_ cred]
    [_ cred callback]
    "Updates the email associated with an email / password user account.")

  (-changepass
    [_ cred]
    [_ cred callback]
    "Changes the password of an existing user using an email / password combination.")

  (-removeuser
    [_ cred]
    [_ cred callback]
    "Removes an existing user account using an email / password combination.")

  (-resetpass
    [_ cred]
    [_ cred callback]
    "Sends a password-reset email to the owner of the account, containing a token that may be used to authenticate and change the user's password.")

  (-goonline
    [_]
    "Manually reestablishes a connection to the Firebase server and enables automatic reconnection.")

  (-gooffline
    [_]
    "Manually disconnects the Firebase client from the server and disables automatic reconnection."))

#?(:clj
    (extend-protocol FirebaseRef

      ;; Firebase Reference
      com.firebase.client.Firebase
      (-parent [ref] (.getParent ref)))

   :cljs
    (extend-protocol FirebaseRef

      ;; Firebase Reference
      js.Firebase
      (-authcustomtoken
        ([ref token]
         (-authcustomtoken ref token undefined))
        ([ref token callback]
         (-authcustomtoken ref token callback nil))
        ([ref token callback opts]
         (prom/promise (.authWithCustomToken ref token callback (clj->js opts)))))

      (-authanonymous
        ([ref callback]
         (-authanonymous ref callback nil))
        ([ref callback opts]
         (prom/promise (.authAnonymously ref callback (clj->js opts)))))

      (-authuserpass
        ([ref cred]
         (-authuserpass ref cred undefined))
        ([ref cred callback]
         (-authuserpass ref cred callback nil))
        ([ref cred callback opts]
         (prom/promise (.authWithPassword ref cred callback opts))))

      (-authoauthpopup
        ([ref provider]
         (-authoauthpopup ref provider undefined))
        ([ref provider callback]
         (-authoauthpopup ref provider callback nil))
        ([ref provider callback opts]
         (prom/promise (.authWithOAuthPopup ref provider callback opts))))

      (-authoauthredirect
        ([ref provider]
         (-authoauthredirect ref provider undefined))
        ([ref provider callback]
         (-authoauthredirect ref provider callback nil))
        ([ref provider callback opts]
         (prom/promise (.authWithOAuthRedirect ref provider callback opts))))

      (-authoauthtoken
        ([ref provider cred]
         (-authoauthtoken ref provider cred undefined))
        ([ref provider cred callback]
         (-authoauthtoken ref provider cred callback nil))
        ([ref provider cred callback opts]
         (prom/promise (.authWithOAuthToken ref provider cred callback opts))))

      (-getauth [ref] (.getAuth ref))

      (-onauth  [ref callback] (.onAuth ref))

      (-offauth [ref callback] (.offAuth ref))

      (-unauth  [ref] (.unauth ref))

      (-parent  [ref] (.parent ref))

      (-root    [ref] (.root ref))

      (-tostr   [ref] (.toString ref))

      (-set
        ([ref value]
         (-set ref value undefined))
        ([ref value callback]
         (prom/promise (.set ref value callback))))

      (-update
        ([ref value]
         (-update ref value undefined))
        ([ref value callback]
         (prom/promise (.update ref value callback))))

      (-remove
        ([ref]
         (-remove ref undefined))
        ([ref callback]
         (prom/promise (.remove ref callback))))

      (-push
        ([ref value]
         (-push ref value undefined))
        ([ref value callback]
         (prom/promise (.push ref value callback))))

      (-setwithpriority
        ([ref value priority]
         (-setwithpriority ref value priority undefined))
        ([ref value priority callback]
         (prom/promise (.setWithPriority ref value priority callback))))

      (-setpriority
        ([ref priority]
         (-setpriority ref priority undefined))
        ([ref priority callback]
         (prom/promise (.setPriority ref priority callback))))

      (-transaction
        ([ref updatefn]
         (-transaction ref updatefn undefined))
        ([ref updatefn callback]
         (-transaction ref updatefn callback false))
        ([ref updatefn callback applylocally]
         (prom/promise (.transaction ref updatefn callback applylocally))))

      (-createuser
        ([ref cred]
         (-createuser ref cred undefined))
        ([ref cred callback]
         (prom/promise (.createUser ref cred callback))))

      (-changeemail
        ([ref cred]
         (-changeemail ref cred undefined))
        ([ref cred callback]
         (prom/promise (.changeEmail ref cred callback))))

      (-changepass
        ([ref cred]
         (-changepass ref cred undefined))
        ([ref cred callback]
         (prom/promise (.changePassword ref cred callback))))

      (-removeuser
        ([ref cred]
         (-removeuser ref cred undefined))
        ([ref cred callback]
         (prom/promise (.removeUser ref cred callback))))

      (-resetpass
        ([ref cred]
         (-resetpass ref cred undefined))
        ([ref cred callback]
         (prom/promise (.resetPassword ref cred callback))))

      (-goonline  [ref] (.goOnline ref))

      (-gooffline [ref] (.goOffline ref))))

;; Firebase Query Protocol
(defprotocol FirebaseQuery
  "A firebase reference query abstraction."

  (-on
    [_ event callback]
    [_ event callback failure]
    "Listens for data changes at a particular location.")

  (-off
    [_ event]
    [_ event callback]
    "Detaches a callback previously attached with on.")

  (-once
    [_ event]
    [_ event callback]
    [_ event callback failure]
    "Listens for exactly one event of the specified event type, and then stops listening.")

  (-orderbychild
    [_ key]
    "Generates a new Query object ordered by the specified child key.")

  (-orderbykey
    [_]
    "Generates a new Query object ordered by key.")

  (-orderbyvalue
    [_]
    "Generates a new Query object ordered by child values.")

  (-orderbypriority
    [_]
    "Generates a new Query object ordered by priority.")

  (-startat
    [_ value]
    [_ value key]
    "Creates a Query with the specified starting point. The generated Query includes children which match the specified starting point.")

  (-endat
    [_ value]
    [_ value key]
    "Creates a Query with the specified ending point. The generated Query includes children which match the specified ending point.")

  (-equalto
    [_ value]
    [_ value key]
    "Creates a Query which includes children which match the specified value.")

  (-limitfirst [_ limit] "Generates a new Query object limited to the first certain number of children.")

  (-limitlast  [_ limit] "Generates a new Query object limited to the last certain number of children."))

#?(:cljs
    (extend-protocol FirebaseQuery

      ;; Firebase Reference
      js.Firebase
      (-on
        ([ref event callback]
         (-on ref event callback undefined))
        ([ref event callback failure]
         (.on ref event callback failure)))

      (-off
        ([ref event]
         (-off ref event undefined))
        ([ref event callback]
         (.off ref event callback)))

      (-once
        ([ref event]
         (-once ref event undefined))
        ([ref event callback]
         (-once ref event callback undefined))
        ([ref event callback failure]
         (prom/promise (.once ref event callback failure))))

      (-orderbychild    [ref key] (.orderByChild ref key))

      (-orderbykey      [ref] (.orderByKey ref))

      (-orderbyvalue    [ref] (.orderByValue ref))

      (-orderbypriority [ref] (.orderByPriority ref))

      (-startat
        ([ref value]
         (-startat ref value nil))
        ([ref value key]
         (.startAt ref value key)))

      (-endat
        ([ref value]
         (-endat ref value nil))
        ([ref value key]
         (.endAt ref value key)))

      (-equalto
        ([ref value]
         (-equalto ref value nil))
        ([ref value key]
         (.equalTo ref value key)))

      (-limitfirst [ref limit] (.limitToFirst ref limit))

      (-limitlast  [ref limit] (.limitToLast ref limit))

      ;; Firebase DataSnapshot
      object
      (-on
        ([dat event callback]
         (-on dat event callback undefined))
        ([dat event callback failure]
         (prom/chain dat -ref #(-on % event callback failure))))

      (-off
        ([dat event]
         (-off dat event undefined))
        ([dat event callback]
         (prom/chain dat -ref #(-off % event callback))))

      (-once
        ([dat event]
         (-once dat event undefined))
        ([dat event callback]
         (-once dat event callback undefined))
        ([dat event callback failure]
         (prom/chain dat -ref #(-once % event callback failure))))

      (-orderbychild    [dat key] (prom/chain dat -ref #(-orderbychild % key)))

      (-orderbykey      [dat] (prom/chain dat -ref #(-orderbykey %)))

      (-orderbyvalue    [dat] (prom/chain dat -ref #(-orderbyvalue %)))

      (-orderbypriority [dat] (prom/chain dat -ref #(-orderbypriority %)))

      (-startat
        ([dat value]
         (-startat dat value nil))
        ([dat value key]
         (prom/chain dat -ref #(-startat % value key))))

      (-endat
        ([dat value]
         (-endat dat value nil))
        ([dat value key]
         (prom/chain dat -ref #(-endat % value key))))

      (-equalto
        ([dat value]
         (-equalto dat value nil))
        ([dat value key]
         (prom/chain dat -ref #(-equalto % value key))))

      (-limitfirst [dat limit] (prom/chain dat -ref #(-limitfirst % limit)))

      (-limitlast  [dat limit] (prom/chain dat -ref #(-limitlast % limit)))))

;; Firebase DataSnapshot Protocol
(defprotocol FirebaseSnapshot
  "A firebase data snapshot abstraction."

  (-exists
    [_]
    "Returns true if this DataSnapshot contains any data.")

  (-foreach
    [_ callback]
    "Enumerates through the DataSnapshot’s children (in the default order).")

  (-haschild
    [_ path]
    "Returns true if the specified child exists.")

  (-haschildren
    [_]
    "Returns true if the DataSnapshot has any children.")

  (-numchildren
    [_]
    "Gets the number of children for this DataSnapshot."))

#?(:cljs
    (extend-protocol FirebaseSnapshot

      ;; Firebase Reference
      js.Firebase
      (-exists      [ref] (prom/then (-once ref "value") -exists))

      (-foreach     [ref callback] (prom/then (-once ref "value") #(-foreach % callback)))

      (-haschild    [ref path] (prom/then (-once ref "value") #(-haschild % path)))

      (-haschildren [ref] (prom/then (-once ref "value") -haschildren))

      (-numchildren [ref] (prom/then (-once ref "value") -numchildren))

      ;; Firebase DataSnapshot
      object
      (-exists      [dat] (.exists dat))

      (-foreach     [dat callback] (.forEach dat callback))

      (-haschild    [dat path] (.hasChild dat path))

      (-haschildren [dat] (.hasChildren dat))

      (-numchildren [dat] (.numChildren dat))))
