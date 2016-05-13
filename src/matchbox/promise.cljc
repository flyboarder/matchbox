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
        (.then (promise it) #(cb %)))
      (-bind
        [it cb]
        (.then (promise it) #(cb %)))
      (-catch
        [it cb]
        (.catch (promise it) #(cb %)))))

#?(:cljs
    ;; Firebase Promise
    (extend-type prom/Promise

      proto/Firebase
      (-ref [p] (then p proto/-ref))

      (-key [p] (then p proto/-key))

      (-val
        ([p] (then p proto/-val))
        ([p callback] (chain p proto/-val callback)))

      (-child [p path] (then p #(proto/-child % path)))

      proto/FirebaseRef
      (-authcustomtoken
        ([p token]
         (then p #(proto/-authcustomtoken % token)))
        ([p token callback]
         (then p #(proto/-authcustomtoken % token callback)))
        ([p token callback opts]
         (then p #(proto/-authcustomtoken % token callback (clj->js opts)))))

      (-authanonymous
        ([p callback]
         (then p #(proto/-authanonymous % callback)))
        ([p callback opts]
         (then p #(proto/-authanonymous % callback (clj->js opts)))))

      (-authuserpass
        ([p cred]
         (then p #(proto/-authuserpass % cred)))
        ([p cred callback]
         (then p #(proto/-authuserpass % cred callback)))
        ([p cred callback opts]
         (then p #(proto/-authuserpass % cred callback opts))))

      (-authoauthpopup
        ([p provider]
         (then p #(proto/-authoauthpopup % provider)))
        ([p provider callback]
         (then p #(proto/-authoauthpopup % provider callback)))
        ([p provider callback opts]
         (then p #(proto/-authoauthpopup % provider callback opts))))

      (-authoauthredirect
        ([p provider]
         (then p #(proto/-authoauthredirect % provider)))
        ([p provider callback]
         (then p #(proto/-authoauthredirect % provider callback)))
        ([p provider callback opts]
         (then p #(proto/-authoauthredirect % provider callback opts))))

      (-authoauthtoken
        ([p provider cred]
         (then p #(proto/-authoauthtoken % provider cred)))
        ([p provider cred callback]
         (then p #(proto/-authoauthtoken % provider cred callback)))
        ([p provider cred callback opts]
         (then p #(proto/-authoauthtoken % provider cred callback opts))))

      (-getauth [p] (then p proto/-getauth))

      (-onauth  [p callback] (then p #(proto/-onauth % callback)))

      (-offauth [p callback] (then p #(proto/-offauth % callback)))

      (-unauth  [p] (then p proto/-unauth))

      (-parent  [p] (then p proto/-parent))

      (-root    [p] (then p proto/-root))

      (-tostr   [p] (then p proto/-tostr))

      (-set
        ([p value]
         (then p #(proto/-set % value)))
        ([p value callback]
         (then p #(proto/-set % value callback))))

      (-update
        ([p value]
         (then p #(proto/-update % value)))
        ([p value callback]
         (then p #(proto/-update % value callback))))

      (-remove
        ([p]
         (then p #(proto/-remove %)))
        ([p callback]
         (then p #(proto/-remove % callback))))

      (-push
        ([p value]
         (then p #(proto/-push % value)))
        ([p value callback]
         (then p #(proto/-push % value callback))))

      (-setwithpriority
        ([p value priority]
         (then p #(proto/-setwithpriority % value priority)))
        ([p value priority callback]
         (then p #(proto/-setwithpriority % value priority callback))))

      (-setpriority
        ([p priority]
         (then p #(proto/-setpriority % priority)))
        ([p priority callback]
         (then p #(proto/-setpriority % priority callback))))

      (-transaction
        ([p updatefn]
         (then p #(proto/-transaction % updatefn)))
        ([p updatefn callback]
         (then p #(proto/-transaction % updatefn callback)))
        ([p updatefn callback applylocally]
         (then p #(proto/-transaction % updatefn callback applylocally))))

      (-createuser
        ([p cred]
         (then p #(proto/-createuser % cred)))
        ([p cred callback]
         (then p #(proto/-createuser % cred callback))))

      (-changeemail
        ([p cred]
         (then p #(proto/-changeemail % cred)))
        ([p cred callback]
         (then p #(proto/-changeemail % cred callback))))

      (-changepass
        ([p cred]
         (then p #(proto/-changepass % cred)))
        ([p cred callback]
         (then p #(proto/-changepass % cred callback))))

      (-removeuser
        ([p cred]
         (then p #(proto/-removeuser % cred)))
        ([p cred callback]
         (then p #(proto/-removeuser % cred callback))))

      (-resetpass
        ([p cred]
         (then p #(proto/-resetpass % cred)))
        ([p cred callback]
         (then p #(proto/-resetpass % cred callback))))

      (-goonline  [p] (then p #(proto/-goonline %)))

      (-gooffline [p] (then p #(proto/-gooffline %)))

      proto/FirebaseQuery
      (-on
        ([p event callback]
         (then p #(proto/-on % event callback)))
        ([p event callback failure]
         (then p #(proto/-on % event callback failure))))

      (-off
        ([p event]
         (then p #(proto/-off % event)))
        ([p event callback]
         (then p #(proto/-off % event callback))))

      (-once
        ([p event]
         (then p #(proto/-once % event)))
        ([p event callback]
         (then p #(proto/-once % event callback)))
        ([p event callback failure]
         (then p #(proto/-once % event callback failure))))

      (-orderbychild    [p key] (then p #(proto/-orderbychild % key)))

      (-orderbykey      [p] (then p #(proto/-orderbykey %)))

      (-orderbyvalue    [p] (then p #(proto/-orderbyvalue %)))

      (-orderbypriority [p] (then p #(proto/-orderbypriority %)))

      (-startat
        ([p value]
         (then p #(proto/-startat % value)))
        ([p value key]
         (then p #(proto/-startat % value key))))

      (-endat
        ([p value]
         (then p #(proto/-endat % value)))
        ([p value key]
         (then p #(proto/-endat % value key))))

      (-equalto
        ([p value]
         (then p #(proto/-equalto % value)))
        ([p value key]
         (then p #(proto/-equalto % value key))))

      (-limitfirst [p limit] (then p #(proto/-limitfirst % limit)))

      (-limitlast  [p limit] (then p #(proto/-limitlast % limit)))

      proto/FirebaseSnapshot
      (-exists      [p] (then p proto/-exists))

      (-foreach     [p callback] (then p #(proto/-foreach % callback)))

      (-haschild    [p path] (then p #(proto/-haschild % path)))

      (-haschildren [p] (then p proto/-haschildren))

      (-numchildren [p] (then p proto/-numchildren))

      ;; Firebase Promise
      mbox/Matchbox
      (get-in
        [p korks]
        (then p #(get-in % korks)))

      (deref
        ([p state]
         (mbox/--deref p state))
        ([p state callback]
         (mbox/--deref p state callback)))

      (deref-list
        ([p state]
         (mbox/--deref-list p state))
        ([p state callback]
         (mbox/--deref-list p state callback)))

      (reset!
        ([p val callback]
         (then p #(mbox/reset! (proto/-ref %) val callback))))

      (-swap!
        ([p fn args]
         (then p #(mbox/swap! % fn args))))

      (merge!
        ([p val callback]
         (then p #(mbox/merge! (proto/-ref %) val callback))))

      (conj!
        ([p val callback]
         (then p #(mbox/conj! (proto/-ref %) val callback))))

      (dissoc!
        ([p]
         (then p #(mbox/dissoc! (proto/-ref %))))
        ([p callback]
         (then p #(mbox/dissoc! (proto/-ref %) callback))))

      (order-priority [p] (then p proto/-orderbypriority))

      (order-key [p] (then p proto/-orderbykey))

      (order-value [p] (then p proto/-orderbyvalue))

      (order-child [p child] (then p #(proto/-orderbychild % child)))

      (start-at
        ([p val]
         (then p #(proto/-startat % val)))
        ([p val key]
         (then p #(proto/-startat % val key))))

      (end-at
        ([p val]
         (then p #(proto/-endat % val)))
        ([p val key]
         (then p #(proto/-endat % val key))))

      (equal-to
        ([p val]
         (then p #(proto/-equalto % val)))
        ([p val key]
         (then p #(proto/-equalto % val key))))


      ))
