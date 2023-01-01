/*
Copyright (c) 2022-2023 Steve Shering

All rights reserved.

As a special exception, the copyright holder of this software gives you permission
to use this software for personal, not-for-profit purposes.

For any other purpose, a license must be obtained from the copyright holder.

This copyright notice and this permission notice must be included in all copies 
of this software, including copies of parts of this software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHOR OR COPYRIGHT HOLDER BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package net.sherst.util;

import java.io.Reader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A simple fast ring buffer (circular buffer) implementation for Java.
 * <p>
 * A ring buffer is a fixed-size first in - first out buffer
 * typically backed by an array with head and tail pointers.
 * Elements are added to the tail of the buffer and removed from the head.
 * When the end of the array is reached, the pointers wrap around to the beginning.
 * When the buffer is full, adding new elements overwrites the oldest elements
 * (but see the {@link offer(E)} and {@link offer(E[])} methods in this implementation).
 * <p>
 * Pros: Fast, simple. 
 * <p>
 * Cons: Size fixed at creation time, 
 * adding new elements overwrites the oldest ones if the buffer is full,
 * elements can only be added or removed in FIFO order.
 * <p>
 * There are some useful look-ahead methods, {@link #peek()}, {@link #peek(int)}, {@link #at(E)},
 * {@link #at(E[])}, {@link #atSkip(E)} 
 * and {@link #atSkip(E[])}.
 * <p>
 * These are simple, hopefully fast, implementations. 
 * They deliberately do not implement Java's {@link java.util.Collection} or {@link java.util.Queue} interfaces,
 * although the API is similar.
 * <p>
 * Variants:<ul>
 * <li>{@link FastRingBuffer}: Simplest, fastest, not thread safe.
 * <li>{@link SafeRingBuffer}: Thread safe.
 * <li>{@link BlockingRingBuffer}: add and remove methods block if necessary; offer and poll methods are non-blocking.
 * </ul>
 * <p>
 * {@link #remove()} sets the underlying array slot to {@code null} to avoid memory leaks.
 * All other {@code remove} and {@code poll} methods call {@link #remove()}. 
 * <p>
 * {@link #clear()} discards the underlying array to avoid memory leaks. 
 * {@link #removeAll()} calls {@link #clear()}. 
 * <p>
 * Elements can be {@code null}, but this can make interpretation of return values of some methods awkward.
 * Consider using {@link #size()} to disambiguate.
 * <p>
 * {@link #poll()} might seem redundant, but is overridden in {@link BlockingRingBuffer}.
 * 
 * @param <E> the type of elements in the buffer
 * @author Steve Shering
 */
public class FastRingBuffer<E> {
  private E[] arr;
  protected int capacity=0;
  protected int count=0;
  private int head=0;
  private int tail=0;
	
	/**
	 * Creates a new buffer.
	 * 
	 * @param capacity Maximum capacity of the buffer
	 */
	public FastRingBuffer(int capacity) {
		this.capacity=capacity;
		clear();
  	}
	
	/**
	 * Adds an element to the buffer.
	 * If the buffer is full, silently overwrites the oldest entry.
	 * 
	 * @param e the entry to add
	 * @return {@code true}
	 */
	public boolean add(E e) {
    arr[tail]=e;
    if (count<capacity) 
    	count++;
    else {
    	head++;
    	if (head==capacity)
    		head=0;
    	}
    tail++;
    if (tail==capacity)
    	tail=0;
    return true;
    }
	
	/**
	 * Adds elements to the buffer, preserving their ordering in the array.
	 * If the buffer is full, silently overwrites the oldest entries.
	 * 
	 * @param a the entries to add
	 * @return {@code true}
	 */
	public boolean addAll(E[] a) {
    for (E e : a)
    	add(e);
    return true;
    }
	
	/**
	 * Adds elements to the buffer, preserving their ordering in the array.
	 * If the buffer is full, silently overwrites the oldest entries.
	 * 
	 * @param c the entries to add
	 * @return {@code true}
	 */
	public boolean addAll(Collection<E> c) {
    for (E e : c)
    	add(e);
    return true;
    }
	
	/**
	 * Compares the head of the buffer (the next entry that will be removed) to {@code o}.
	 * 
	 * @param o the value to be compared
	 * @return {@code true} if the head of the buffer equals {@code o}; 
	 * {@code false} if the buffer is empty
	 */
	public boolean at(E o) {
		if (count==0)
			return false;
		return Objects.equals(arr[head], o);
		}
	
	/**
	 * Compares entries at the head of the buffer (the next entries that will be removed) to {@code a}.
	 * 
	 * @param a the values to be compared
	 * @return {@code true} if the entries at head of the buffer are equal to the elements of {@code a}; 
	 * {@code false} if the buffer doesn't contain enough entries to match.
	 */
	public boolean at(E[] a) {
		if (count<a.length)
			return false;
		for (int i=0; i<a.length; i++) {
			if (!Objects.equals(peek(i), a[i]))
				return false;
		  }
		return true;
		}
	
	/**
	 * Compares the head of the buffer (the next entry that will be removed) to {@code o}. 
	 * Removes it if it matches. 
	 * 
	 * @param o the value to be compared
	 * @return {@code true} if the head of the buffer equals {@code o} and is removed; 
	 * {@code false} if the buffer is empty
	 */
	public boolean atSkip(E o) {
		if (count==0)
			return false;
		if (arr[head]!=o) 
		  return false;
		remove();
		return true;
		}
	
	/**
	 * Compares the head of the buffer (the next entries that will be removed) to {@code a}. 
	 * Removes them if they match. 
	 * 
	 * @param a the values to be compared
	 * @return {@code true} if the entries at head of the buffer are equal to the elements of {@code a}; 
	 * {@code false} if the buffer doesn't contain enough entries to match.
	 */
	public boolean atSkip(E[] a) {
		if (!at(a))
		  return false;
		skip(a.length);
		return true;
		}
	
	/**
	 * Empties the buffer.
	 */
	public void clear() {
		arr=(E[]) new Object[capacity];
		head=0;
		tail=0;
		count=0;
		}
	
	/**
	 * Returns {@code true} if the buffer contains {@code e}, tested using Objects.equals(). 
	 * 
	 * @param {@code e} the value to be tested
	 * @return {@code true} if the buffer contains {@code e}; 
	 * {@code false} if not.
	 */
	public boolean contains(E e) {
		for (int i=0; i<count; i++) 
			if (Objects.equals(e, arr[(i+head)%capacity]))
				return true;
		return false;
		}
	
	/**
	 * Returns the maximum capacity of this buffer.
	 * 
	 * @return the maximum capacity of this buffer
	 */
	public int getCapacity() {
		return capacity;
		}
	
	/**
	 * Returns {@code true} if the buffer is empty.
	 * 
	 * @return {@code true} if the buffer is empty
	 */
	public boolean isEmpty() {
	  return count==0;
    }
	
	/**
	 * Adds an entry to the buffer if there is space for it.
	 * 
	 * @param e the entry to add.
	 * @return {@code true} if there was space in the buffer and the entry was added
	 */
	public boolean offer(E e) {
	  if (count>=capacity)
	  	return false;
	  add(e);
	  return true;
	  }
	
	/**
	 * Adds entries to the buffer if there is space for them.
	 * 
	 * @param e the entry to add.
	 * @return {@code true} if there was space in the buffer and the entries were added
	 */
	public boolean offer(E[] a) {
	  if (count+a.length>capacity)
	  	return false;
	  for (E e : a)
	    add(e);
	  return true;
	  }
	
	/**
	 * Adds entries to the buffer if there is space for them.
	 * 
	 * @param e the entry to add.
	 * @return {@code true} if there was space in the buffer and the entries were added
	 */
	public boolean offer(Collection<E> c) {
	  if (count+c.size()>capacity)
	  	return false;
	  for (E e : c)
	    add(e);
	  return true;
	  }
		
	/**
	 * Returns the head of the buffer (the next element that will be removed) without actually removing it.
	 * 
	 * @return the element at the head of the buffer or {@code null} if the buffer is empty
	 */
	public E peek() {
		if (count==0)
			return null;
		return arr[head];
	  }
	
	/**
	 * Returns the {@code n}<i>th</i> element that will be removed from the buffer, 
	 * counting from 0 ({@code peek(0)} returns the first element that will be removed), 
	 * or {@code null} if the buffer does not contain that many elements.
	 * 
	 * @param n
	 * @return the {@code n}<i>th</i> element that will be removed from the buffer 
	 * or {@code null} if the buffer does not contain that many elements
	 */
	public E peek(int n) {
		if (n>count)
			return null;
		n+=head;
		if (n>=capacity)
			n-=capacity;
		return arr[n];
	  }
	
	/**
	 * Removes and returns the element at the head of the buffer, 
	 * or {@code null} if the buffer is empty.  
	 * 
	 * @return the element that was at the head of the buffer which was removed, 
	 * or {@code null} if the buffer is empty
	 */
	public E poll() {
		if (count<1)
			return null;
		return remove();
	  }
	
	/**
	 * Removes and returns {@code n} consecutive elements from the head of the buffer 
	 * in the order they were added, or {@code null} if the buffer does not contain {@code n} elements. 
	 * 
	 * @param n the number of elements to remove
	 * @param cls the {@code Class} of the elements in the buffer (needed to create the array)
	 * @return the elements that were at the head of the buffer which were removed, 
	 * or {@code null} if the buffer does not contain {@code n} elements
	 */
	public E[] poll(int n, Class<E> cls) {
		if (count<n)
			return null;
		return remove(n, cls);
	  }
	
	/**
	 * Removes and returns the element at the head of the buffer.
	 * 
	 * @return the element that was at the head of the buffer which was removed, 
	 * or {@code null} if the buffer is empty
	 */
	public E remove() {
		if (count==0)
			return null;
		E r=arr[head];
		arr[head]=null;
		head++;
		if (head==capacity)
			head=0;
		count--;
		return r;
	  }
	
	/**
	 * Removes and returns up to {@code n} consecutive elements from the head of the buffer 
	 * in the order they were added.
	 * 
	 * @param n the number of elements to remove
	 * @param cls the {@code Class} of the elements in the buffer (needed to create the array)
	 * @return the elements that were at the head of the buffer which were removed; 
	 * the length of the array reflects the number of elements that were available 
	 */
	public E[] remove(int n, Class<E> cls) {
		n=Math.min(n, count);
		E[] r=(E[]) Array.newInstance(cls, n);
		for (int i=0; i<n; i++)
			r[i]=remove();
		return r;
	  }
	
	/**
	 * Empties the buffer.
	 * 
	 * @return {@code true}  
	 */
	public boolean removeAll() {
		clear();
	  return true;
	  }
	
	/**
	 * Returns the number of elements in the buffer.
	 * 
	 * @return the number of elements in the buffer
	 */
	public int size() {
		return count;
		}
	
	/**
	 * Removes {@code n} elements from the head of the buffer (and discards them). 
	 * If {@code n}&gt;{@link size()}, only {@link size()} elements are removed. 
	 * Inspired by and named after similar methods in 
	 * {@link java.io.InputStream}s and {@link Reader}s. 
	 * 
	 * @param n the number of elements to remove
	 * @return the number of elements actually removed
	 */
	public int skip(int n) {
		if (n<0)
			throw new IllegalArgumentException("n<0");
		if (n>count)
			n=count;
		//this probably needs a loop to do arr[i]=null
    for (int i=0; i<n; i++)
    	remove();
    return n;
    }
	
	public List<E> toList() {
	  var list=new ArrayList<E>();
		for (int i=0; i<count; i++) 
			list.add(arr[(i+head)%capacity]);
		return list;
	  }
  }
