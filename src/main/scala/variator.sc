def ll(a:Int):LazyList[Int] = a#::ll(a+1)
ll(0).take(40).force