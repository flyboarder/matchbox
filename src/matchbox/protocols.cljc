(ns matchbox.protocols
  ;(:refer-clojure :exclude [])
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [promesa.core :as prom]
    [matchbox.core :as mbox]
    [matchbox.utils :as utils]))

;; Matchbox Public API Protocol
(defprotocol Matchbox
  "Matchbox Public API"

  (get-in
    [_ korks]
    "Obtain child Reference or DataSnapshot from base by following korks.")

  (parent
    [_]
    "Immediate ancestor of Reference or DataSnapshot, if any.")

  (-deref
    [_]
    [_ state]
    [_ state callback]
    "Deref a Reference, Promise or DataSnapshot.")

  (deref-list
    [_]
    [_ state]
    [_ state callback]
    "Deref a Reference, Promise or DataSnapshot and return a list of values.")

  (reset-priority!
    [_ priority]
    [_ priority callback]
    "Set priority value on Reference, Promise or DataSnapshot.")

  (-reset!
    [_ val]
    [_ val callback]
    "Reset value on Reference, Promise or DataSnapshot.")

  (-swap!
    [_ fn args]
    "Swap the value on a Reference, Promise or DataSnapshot.")

  (-merge!
    [_ val]
    [_ val callback]
    "Merge a value with a Reference, Promise or DataSnapshot.")

  (-conj!
    [_ val]
    [_ val callback]
    "Conjoin a value to a Reference, Promise or DataSnapshot.")

  (-dissoc!
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
