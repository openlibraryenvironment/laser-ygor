package de.hbznrw.ygor.iet.export.structure

import java.util.ArrayList

import de.hbznrw.ygor.iet.enums.*

class TippTitle {
    
    Pod name        = new Pod("")
    Pod type        = new Pod("Serial", Status.HARDCODED)
    
    ArrayList<Identifier> identifiers = []
}