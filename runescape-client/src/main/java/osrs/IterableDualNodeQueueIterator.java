package osrs;

import java.util.Iterator;
import net.runelite.mapping.Export;
import net.runelite.mapping.Implements;
import net.runelite.mapping.ObfuscatedName;
import net.runelite.mapping.ObfuscatedSignature;

@ObfuscatedName("ld")
@Implements("IterableDualNodeQueueIterator")
public class IterableDualNodeQueueIterator implements Iterator {
   @ObfuscatedName("i")
   @ObfuscatedSignature(
      descriptor = "Llm;"
   )
   @Export("queue")
   IterableDualNodeQueue queue;
   @ObfuscatedName("w")
   @ObfuscatedSignature(
      descriptor = "Lnt;"
   )
   @Export("head")
   DualNode head;
   @ObfuscatedName("s")
   @ObfuscatedSignature(
      descriptor = "Lnt;"
   )
   @Export("last")
   DualNode last = null;

   @ObfuscatedSignature(
      descriptor = "(Llm;)V"
   )
   IterableDualNodeQueueIterator(IterableDualNodeQueue var1) {
      this.queue = var1;
      this.head = this.queue.sentinel.previousDual;
      this.last = null;
   }

   public boolean hasNext() {
      return this.queue.sentinel != this.head;
   }

   public Object next() {
      DualNode var1 = this.head;
      if (var1 == this.queue.sentinel) {
         var1 = null;
         this.head = null;
      } else {
         this.head = var1.previousDual;
      }

      this.last = var1;
      return var1;
   }

   public void remove() {
      if (this.last == null) {
         throw new IllegalStateException();
      } else {
         this.last.removeDual();
         this.last = null;
      }
   }
}
