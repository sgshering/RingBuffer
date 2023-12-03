# RingBuffer

Simple, fast ring buffer (circular buffer) implementations for Java.

A ring buffer is a fixed-size first in - first out buffer typically backed by an array with head and tail pointers. Elements are added to the tail of the buffer and removed from the head. When the end of the array is reached, the pointers wrap around to the beginning. When the buffer is full, adding new elements overwrites the oldest elements (but see the **offer(E)** and **offer(E[])** methods in these implementations).

The javadocs are [here](https://www.sherst.net/javadoc/net/sherst/util/package-summary.html)

Pros: Fast, simple.

Cons: Size fixed at creation time, adding new elements overwrites the oldest ones if the buffer is full, elements can only be added or removed in FIFO order.

There are some useful look-ahead methods, **peek()**, **peek(int)**, **at(E)**, **at(E[])**, **atSkip(E)** and **atSkip(E[])**.

These are simple, hopefully fast, implementations. They deliberately do not implement Java's **Collection** or **Queue** interfaces, although the API is similar.

Variants:

* FastRingBuffer: Simplest, fastest, not thread safe.
* SafeRingBuffer: Thread safe.
* BlockingRingBuffer: **add** and **remove** methods block if necessary; **offer** and **poll** methods are non-blocking. 

**remove()** sets the underlying array slot to null to avoid memory leaks. All other remove and poll methods call **remove()**.

**clear()** discards the underlying array to avoid memory leaks. **removeAll()** calls **clear()**.

Elements can be **null**, but this means that, if **peek()**, **poll()** or **remove()** return **null**, this could be because an element was **null** or because the buffer was empty. Consider using **isEmpty()** or **size()** to disambiguate.

**poll()** might seem redundant, but is overridden in **BlockingRingBuffer**.

Author: sherstDotNet@yahoo.com
