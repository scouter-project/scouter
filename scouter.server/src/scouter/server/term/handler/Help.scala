package scouter.server.term.handler;

import scouter.server.term.ScouterHandler

object Help {
    def help(cmd:String):Unit= {
        System.out.println("\thelp = Help");
        System.out.println("\tquit = Quit");
        System.out.println("\tobjtypes = ObjType List");
        System.out.println("\tobjects = Object List");
       System.out.println("\tcounters [objType] = counter list for the objType");
    }

    def quit() :Unit= {
        System.out.println("bye bye!!");
        System.exit(1);
    }

}
