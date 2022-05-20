package MapReduce.SimpleSplit

import scala.collection.immutable.Iterable
import scala.concurrent.Future

object SplitExecution {
  def apply[In,Out](
                data: Iterable[ In ],
                mf: Iterable[ In ] => Iterable[ Iterable[ In ] ],
                rf: (Out, Out) => Out,
                f: In => Out
              ):Future[Out] = ClusterInteractions.start(data,mf,rf,f)
}
