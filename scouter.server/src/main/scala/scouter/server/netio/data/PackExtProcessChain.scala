package scouter.server.netio.data

import java.util

import scouter.lang.pack.Pack
import scouter.server.util.EnumerScala

/**
  * Pack processor extension chain
  * Created by LeeGunHee on 2016-03-08.
  */
object PackExtProcessChain {

    val processorList = new util.ArrayList[IPackProcessor]

    def addProsessor(processor: IPackProcessor): Unit = {
        processorList.add(processor)
    }

    def doChain(pack: Pack): Unit = {
        var isMatched = false;
        EnumerScala.forward(processorList, (processor: IPackProcessor) => {
            val matched = processor.process(pack);
            if(matched) {
                isMatched = true
            }
        })

        if(!isMatched) {
            System.out.println(pack)
        }
    }
}
