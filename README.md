# ClusterFall
Пытаюсь написать систему на основе akka-cluster так, чтобы можно было раскидывать произвольные задачи между различными компьютерами. При этом акторы-исполнители в такой системе должны быть максимально простыми: единственная функциональность, доступная им должна состоять в том, чтобы принимать задачи на исполнение, исполнять их и возвращать результат.
