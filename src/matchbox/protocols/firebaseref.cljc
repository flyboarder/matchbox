(ns matchbox.protocols.firebaseref
  (:refer-clojure :exclude [])
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [promesa.core :as prom]
    [matchbox.core :as mbox]
    [matchbox.protocols :as proto]
    [matchbox.utils :as utils]))

#?(:cljs
    (extend-protocol proto/FirebaseRef

      ;; Firebase Reference
      js.Firebase
      (-authcustomtoken
        ([ref token]
         (-authcustomtoken ref token matchbox.core/undefined))
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
         (-authuserpass ref cred matchbox.core/undefined))
        ([ref cred callback]
         (-authuserpass ref cred callback nil))
        ([ref cred callback opts]
         (prom/promise (.authWithPassword ref cred callback opts))))

      (-authoauthpopup
        ([ref provider]
         (-authoauthpopup ref provider matchbox.core/undefined))
        ([ref provider callback]
         (-authoauthpopup ref provider callback nil))
        ([ref provider callback opts]
         (prom/promise (.authWithOAuthPopup ref provider callback opts))))

      (-authoauthredirect
        ([ref provider]
         (-authoauthredirect ref provider matchbox.core/undefined))
        ([ref provider callback]
         (-authoauthredirect ref provider callback nil))
        ([ref provider callback opts]
         (prom/promise (.authWithOAuthRedirect ref provider callback opts))))

      (-authoauthtoken
        ([ref provider cred]
         (-authoauthtoken ref provider cred matchbox.core/undefined))
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
         (-set ref value matchbox.core/undefined))
        ([ref value callback]
         (prom/promise (.set ref value callback))))

      (-update
        ([ref value]
         (-update ref value matchbox.core/undefined))
        ([ref value callback]
         (prom/promise (.update ref value callback))))

      (-remove
        ([ref]
         (-remove ref matchbox.core/undefined))
        ([ref callback]
         (prom/promise (.remove ref callback))))

      (-push
        ([ref value]
         (-push ref value matchbox.core/undefined))
        ([ref value callback]
         (prom/promise (.push ref value callback))))

      (-setwithpriority
        ([ref value priority]
         (-setwithpriority ref value priority matchbox.core/undefined))
        ([ref value priority callback]
         (prom/promise (.setWithPriority ref value priority callback))))

      (-setpriority
        ([ref priority]
         (-setpriority ref priority matchbox.core/undefined))
        ([ref priority callback]
         (prom/promise (.setPriority ref priority callback))))

      (-transaction
        ([ref updatefn]
         (-transaction ref updatefn matchbox.core/undefined))
        ([ref updatefn callback]
         (-transaction ref updatefn callback false))
        ([ref updatefn callback applylocally]
         (prom/promise (.transaction ref updatefn callback applylocally))))

      (-createuser
        ([ref cred]
         (-createuser ref cred matchbox.core/undefined))
        ([ref cred callback]
         (prom/promise (.createUser ref cred callback))))

      (-changeemail
        ([ref cred]
         (-changeemail ref cred matchbox.core/undefined))
        ([ref cred callback]
         (prom/promise (.changeEmail ref cred callback))))

      (-changepass
        ([ref cred]
         (-changepass ref cred matchbox.core/undefined))
        ([ref cred callback]
         (prom/promise (.changePassword ref cred callback))))

      (-removeuser
        ([ref cred]
         (-removeuser ref cred matchbox.core/undefined))
        ([ref cred callback]
         (prom/promise (.removeUser ref cred callback))))

      (-resetpass
        ([ref cred]
         (-resetpass ref cred matchbox.core/undefined))
        ([ref cred callback]
         (prom/promise (.resetPassword ref cred callback))))

      (-goonline  [ref] (.goOnline ref))

      (-gooffline [ref] (.goOffline ref))))
