package scouter.server.netio.service.net

object Test {

    def main(args: Array[String]): Unit = {
        val a =3
        
        a match {
            case 1 | 2 => println("a=>"+a)
            case _ =>  println("not match")
        }
        
    }

}