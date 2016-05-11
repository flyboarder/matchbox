(ns matchbox.protocols.firebase
  (:refer-clojure :exclude [])
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [promesa.core :as prom]
    [matchbox.core :as mbox]
    [matchbox.protocols :as proto]
    [matchbox.utils :as utils]))

#?(:cljs
    (extend-protocol proto/Firebase

      ;; Firebase Reference
      js.Firebase
      (-ref [ref] (.ref ref))

      (-key [ref] (.key ref))

      (-val
        ([ref] (prom/then (-once ref "value") -val))
        ([ref callback] (-once ref "value" callback)))

      (-child [ref path] (.child ref path))

      ;; Firebase DataSnapshot
      object
      (-ref [dat] (.ref dat))

      (-key [dat] (.key dat))

      (-val
        ([dat] (.val dat))
        ([dat callback] (comp (callback (.val dat))
                              (.val dat))))

      (-child [dat path] (.child dat path))))
