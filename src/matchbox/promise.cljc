(ns matchbox.promise
  (:refer-clojure :exclude [promise -key -val map])
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [promesa.core :as prom]
    [matchbox.utils :as utils]))

;; this namespace should only be used with javascript (for now...)

(def promise prom/promise)

(def then prom/then)

(def catch prom/catch)

(def map prom/map)

(def chain prom/chain)

;; Firebase Common Protocol
#?(:cljs
    (defprotocol IFirebase
      "Common firebase abstractions."

      (-ref   [_] "Gets a Firebase reference to the location.")
      (-key   [_] "Returns the last token in a Firebase location.")
      (-val   [_] "Gets the JavaScript object representation of the Reference or DataSnapshot.")
      (-child [_ path] "Gets a Firebase Reference or DataSnapshot for the location at the specified relative path.")))

;; Firebase Reference Protocol
#?(:cljs
    (defprotocol IFirebaseRef
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

      (-getauth [_] "Synchronously retrieves the current authentication state of the client.")

      (-onauth  [_ callback] "Listens for changes to the client's authentication state.")

      (-offauth [_ callback] "Detaches a callback previously attached with onauth.")

      (-unauth  [_] "Unauthenticates a Firebase client.")

      (-parent  [_] "Gets a Firebase reference to the parent location.")

      (-root    [_] "Gets a Firebase reference to the root of the Firebase.")

      (-tostr   [_] "Gets the absolute URL corresponding to this Firebase reference's location.")

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

      (-goonline  [_] "Manually reestablishes a connection to the Firebase server and enables automatic reconnection.")

      (-gooffline [_] "Manually disconnects the Firebase client from the server and disables automatic reconnection.")))

;; Firebase Query Protocol
#?(:cljs
    (defprotocol IFirebaseQuery
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

      (-orderbychild    [_ key] "Generates a new Query object ordered by the specified child key.")

      (-orderbykey      [_] "Generates a new Query object ordered by key.")

      (-orderbyvalue    [_] "Generates a new Query object ordered by child values.")

      (-orderbypriority [_] "Generates a new Query object ordered by priority.")

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

      (-limitlast  [_ limit] "Generates a new Query object limited to the last certain number of children.")))

;; Firebase DataSnapshot Protocol
#?(:cljs
    (defprotocol IFirebaseSnapshot
      "A firebase data snapshot abstraction."

      (-exists      [_] "Returns true if this DataSnapshot contains any data.")

      (-foreach     [_ callback] "Enumerates through the DataSnapshot’s children (in the default order).")

      (-haschild    [_ path] "Returns true if the specified child exists.")

      (-haschildren [_] "Returns true if the DataSnapshot has any children.")

      (-numchildren [_] "Gets the number of children for this DataSnapshot.")))

#?(:cljs
    (extend-protocol IFirebase

      ;; Firebase Reference
      js.Firebase
      (-ref [ref] (.ref ref))

      (-key [ref] (.key ref))

      (-val [ref] (then (-once ref "value") -val))

      (-child [ref path] (.child ref path))

      ;; Firebase Promise
      prom/Promise
      (-ref [p] (then p -ref))

      (-key [p] (then p -key))

      (-val [p] (then p -val))

      (-child [p path] (then p #(-child % path)))

      ;; Firebase DataSnapshot
      object
      (-ref [p] (.ref p))

      (-key [p] (.key p))

      (-val [p] (.val p))

      (-child [p path] (.child p path))))

#?(:cljs
    (extend-protocol IFirebaseRef

      ;; Firebase Reference
      js.Firebase
      (-authcustomtoken
        ([ref token]
         (-authcustomtoken ref token matchbox.core/undefined))
        ([ref token callback]
         (-authcustomtoken ref token callback nil))
        ([ref token callback opts]
         (promise (.authWithCustomToken ref token callback (clj->js opts)))))

      (-authanonymous
        ([ref callback]
         (-authanonymous ref callback nil))
        ([ref callback opts]
         (promise (.authAnonymously ref callback (clj->js opts)))))

      (-authuserpass
        ([ref cred]
         (-authuserpass ref cred matchbox.core/undefined))
        ([ref cred callback]
         (-authuserpass ref cred callback nil))
        ([ref cred callback opts]
         (promise (.authWithPassword ref cred callback opts))))

      (-authoauthpopup
        ([ref provider]
         (-authoauthpopup ref provider matchbox.core/undefined))
        ([ref provider callback]
         (-authoauthpopup ref provider callback nil))
        ([ref provider callback opts]
         (promise (.authWithOAuthPopup ref provider callback opts))))

      (-authoauthredirect
        ([ref provider]
         (-authoauthredirect ref provider matchbox.core/undefined))
        ([ref provider callback]
         (-authoauthredirect ref provider callback nil))
        ([ref provider callback opts]
         (promise (.authWithOAuthRedirect ref provider callback opts))))

      (-authoauthtoken
        ([ref provider cred]
         (-authoauthtoken ref provider cred matchbox.core/undefined))
        ([ref provider cred callback]
         (-authoauthtoken ref provider cred callback nil))
        ([ref provider cred callback opts]
         (promise (.authWithOAuthToken ref provider cred callback opts))))

      (-getauth [ref] (.getAuth ref))

      (-onauth  [ref callback] (.onAuth ref))

      (-offauth [ref callback] (.offAuth ref))

      (-unauth  [ref] (.unauth ref))

      (-parent  [ref] (.parent ref))

      (-root    [ref] (.root ref))

      (-tostr   [ref] (.toString ref))

      (-set
        ([ref value]
         (-set ref value matchbox.core/undefined))
        ([ref value callback]
         (promise (.set ref value callback))))

      (-update
        ([ref value]
         (-update ref value matchbox.core/undefined))
        ([ref value callback]
         (promise (.update ref value callback))))

      (-remove
        ([ref]
         (-remove ref matchbox.core/undefined))
        ([ref callback]
         (promise (.remove ref callback))))

      (-push
        ([ref value]
         (-push ref value matchbox.core/undefined))
        ([ref value callback]
         (promise (.push ref value callback))))

      (-setwithpriority
        ([ref value priority]
         (-setwithpriority ref value priority matchbox.core/undefined))
        ([ref value priority callback]
         (promise (.setWithPriority ref value priority callback))))

      (-setpriority
        ([ref priority]
         (-setpriority ref priority matchbox.core/undefined))
        ([ref priority callback]
         (promise (.setPriority ref priority callback))))

      (-transaction
        ([ref updatefn]
         (-transaction ref updatefn matchbox.core/undefined))
        ([ref updatefn callback]
         (-transaction ref updatefn callback false))
        ([ref updatefn callback applylocally]
         (promise (.transaction ref updatefn callback applylocally))))

      (-createuser
        ([ref cred]
         (-createuser ref cred matchbox.core/undefined))
        ([ref cred callback]
         (promise (.createUser ref cred callback))))

      (-changeemail
        ([ref cred]
         (-changeemail ref cred matchbox.core/undefined))
        ([ref cred callback]
         (promise (.changeEmail ref cred callback))))

      (-changepass
        ([ref cred]
         (-changepass ref cred matchbox.core/undefined))
        ([ref cred callback]
         (promise (.changePassword ref cred callback))))

      (-removeuser
        ([ref cred]
         (-removeuser ref cred matchbox.core/undefined))
        ([ref cred callback]
         (promise (.removeUser ref cred callback))))

      (-resetpass
        ([ref cred]
         (-resetpass ref cred matchbox.core/undefined))
        ([ref cred callback]
         (promise (.resetPassword ref cred callback))))

      (-goonline  [ref] (.goOnline ref))

      (-gooffline [ref] (.goOffline ref))

      ;; Firebase Promise
      prom/Promise
      (-authcustomtoken
        ([p token]
         (-authcustomtoken p token matchbox.core/undefined))
        ([p token callback]
         (-authcustomtoken p token callback nil))
        ([p token callback opts]
         (then p #(-authcustomtoken % token callback (clj->js opts)))))

      (-authanonymous
        ([p callback]
         (-authanonymous p callback nil))
        ([p callback opts]
         (then p #(-authanonymous % callback (clj->js opts)))))

      (-authuserpass
        ([p cred]
         (-authuserpass p cred matchbox.core/undefined))
        ([p cred callback]
         (-authuserpass p cred callback nil))
        ([p cred callback opts]
         (then p #(-authuserpass % cred callback opts))))

      (-authoauthpopup
        ([p provider]
         (-authoauthpopup p provider matchbox.core/undefined))
        ([p provider callback]
         (-authoauthpopup p provider callback nil))
        ([p provider callback opts]
         (then p #(-authoauthpopup % provider callback opts))))

      (-authoauthredirect
        ([p provider]
         (-authoauthredirect p provider matchbox.core/undefined))
        ([p provider callback]
         (-authoauthredirect p provider callback nil))
        ([p provider callback opts]
         (then p #(-authoauthredirect % provider callback opts))))

      (-authoauthtoken
        ([p provider cred]
         (-authoauthtoken p provider cred matchbox.core/undefined))
        ([p provider cred callback]
         (-authoauthtoken p provider cred callback nil))
        ([p provider cred callback opts]
         (then p #(-authoauthtoken % provider cred callback opts))))

      (-getauth [p] (then p -getauth))

      (-onauth  [p callback] (then p #(-onauth % callback)))

      (-offauth [p callback] (then p #(-offauth % callback)))

      (-unauth  [p] (then p -unauth))

      (-parent  [p] (then p -parent))

      (-root    [p] (then p -root))

      (-tostr   [p] (then p -tostr))

      (-set
        ([p value]
         (-set p value matchbox.core/undefined))
        ([p value callback]
         (then p #(-set % value callback))))

      (-update
        ([p value]
         (-update p value matchbox.core/undefined))
        ([p value callback]
         (then p #(-update % value callback))))

      (-remove
        ([p]
         (-remove p matchbox.core/undefined))
        ([p callback]
         (then p #(-remove % callback))))

      (-push
        ([p value]
         (-push p value matchbox.core/undefined))
        ([p value callback]
         (then p #(-push % value callback))))

      (-setwithpriority
        ([p value priority]
         (-setwithpriority p value priority matchbox.core/undefined))
        ([p value priority callback]
         (then p #(-setwithpriority % value priority callback))))

      (-setpriority
        ([p priority]
         (-setpriority p priority matchbox.core/undefined))
        ([p priority callback]
         (then p #(-setpriority % priority callback))))

      (-transaction
        ([p updatefn]
         (-transaction p updatefn matchbox.core/undefined))
        ([p updatefn callback]
         (-transaction p updatefn callback false))
        ([p updatefn callback applylocally]
         (then p #(-transaction % updatefn callback applylocally))))

      (-createuser
        ([p cred]
         (-createuser p cred matchbox.core/undefined))
        ([p cred callback]
         (then p #(-createuser % cred callback))))

      (-changeemail
        ([p cred]
         (-changeemail p cred matchbox.core/undefined))
        ([p cred callback]
         (then p #(-changeemail % cred callback))))

      (-changepass
        ([p cred]
         (-changepass p cred matchbox.core/undefined))
        ([p cred callback]
         (then p #(-changepass % cred callback))))

      (-removeuser
        ([p cred]
         (-removeuser p cred matchbox.core/undefined))
        ([p cred callback]
         (then p #(-removeuser % cred callback))))

      (-resetpass
        ([p cred]
         (-resetpass p cred matchbox.core/undefined))
        ([p cred callback]
         (then p #(-resetpass % cred callback))))

      (-goonline  [p] (then p #(-goonline %)))

      (-gooffline [p] (then p #(-gooffline %)))))

#?(:cljs
    (extend-protocol IFirebaseQuery

      ;; Firebase Reference
      js.Firebase
      (-on
        ([ref event callback]
         (-on ref event callback nil))
        ([ref event callback failure]
         (.on ref event callback failure)))

      (-off
        ([ref event]
         (-off ref event matchbox.core/undefined))
        ([ref event callback]
         (.off ref event callback)))

      (-once
        ([ref event]
         (-once ref event matchbox.core/undefined))
        ([ref event callback]
         (-once ref event callback matchbox.core/undefined))
        ([ref event callback failure]
         (promise (.once ref event callback failure))))

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

      ;; Firebase Promise
      prom/Promise
      (-on
        ([p event callback]
         (-on p event callback matchbox.core/undefined))
        ([p event callback failure]
         (then p #(-on % event callback failure))))

      (-off
        ([p event]
         (-off p event matchbox.core/undefined))
        ([p event callback]
         (then p #(-off % event callback))))

      (-once
        ([p event]
         (-once p event matchbox.core/undefined))
        ([p event callback]
         (-once p event callback matchbox.core/undefined))
        ([p event callback failure]
         (then p #(-once % event callback failure))))

      (-orderbychild    [p key] (then p #(-orderbychild % key)))

      (-orderbykey      [p] (then p #(-orderbykey %)))

      (-orderbyvalue    [p] (then p #(-orderbyvalue %)))

      (-orderbypriority [p] (then p #(-orderbypriority %)))

      (-startat
        ([p value]
         (-startat p value nil))
        ([p value key]
         (then p #(-startat % value key))))

      (-endat
        ([p value]
         (-endat p value nil))
        ([p value key]
         (then p #(-endat % value key))))

      (-equalto
        ([p value]
         (-equalto p value nil))
        ([p value key]
         (then p #(-equalto % value key))))

      (-limitfirst [p limit] (then p #(-limitfirst % limit)))

      (-limitlast  [p limit] (then p #(-limitlast % limit)))

      ;; Firebase DataSnapshot
      object
      (-on
        ([dat event callback]
         (-on dat event callback matchbox.core/undefined))
        ([dat event callback failure]
         (chain dat -ref #(-on % event callback failure))))

      (-off
        ([dat event]
         (-off dat event matchbox.core/undefined))
        ([dat event callback]
         (chain dat -ref #(-off % event callback))))

      (-once
        ([dat event]
         (-once dat event matchbox.core/undefined))
        ([dat event callback]
         (-once dat event callback matchbox.core/undefined))
        ([dat event callback failure]
         (chain dat -ref #(-once % event callback failure))))

      (-orderbychild    [dat key] (chain dat -ref #(-orderbychild % key)))

      (-orderbykey      [dat] (chain dat -ref #(-orderbykey %)))

      (-orderbyvalue    [dat] (chain dat -ref #(-orderbyvalue %)))

      (-orderbypriority [dat] (chain dat -ref #(-orderbypriority %)))

      (-startat
        ([dat value]
         (-startat dat value nil))
        ([dat value key]
         (chain dat -ref #(-startat % value key))))

      (-endat
        ([dat value]
         (-endat dat value nil))
        ([dat value key]
         (chain dat -ref #(-endat % value key))))

      (-equalto
        ([dat value]
         (-equalto dat value nil))
        ([dat value key]
         (chain dat -ref #(-equalto % value key))))

      (-limitfirst [dat limit] (chain dat -ref #(-limitfirst % limit)))

      (-limitlast  [dat limit] (chain dat -ref #(-limitlast % limit)))))


#?(:cljs
    (extend-protocol IFirebaseSnapshot

      ;; Firebase Reference
      js.Firebase
      (-exists      [ref] (then (-once ref "value") -exists))

      (-foreach     [ref callback] (then (-once ref "value") #(-foreach % callback)))

      (-haschild    [ref path] (then (-once ref "value") #(-haschild % path)))

      (-haschildren [ref] (then (-once ref "value") -haschildren))

      (-numchildren [ref] (then (-once ref "value") -numchildren))

      ;;Firebase DataSnapshot Promise
      prom/Promise
      (-exists      [p] (then p -exists))

      (-foreach     [p callback] (then p #(-foreach % callback)))

      (-haschild    [p path] (then p #(-haschild % path)))

      (-haschildren [p] (then p -haschildren))

      (-numchildren [p] (then p -numchildren))

      ;; Firebase DataSnapshot
      object
      (-exists      [dat] (.exists dat))

      (-foreach     [dat callback] (.forEach dat callback))

      (-haschild    [dat path] (.hasChild dat path))

      (-haschildren [dat] (.hasChildren dat))

      (-numchildren [dat] (.numChildren dat))))
