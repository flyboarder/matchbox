(ns matchbox.protocols.firebasequery
  (:refer-clojure :exclude [])
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [promesa.core :as prom]
    [matchbox.core :as mbox]
    [matchbox.protocols :as proto]
    [matchbox.utils :as utils]))

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
