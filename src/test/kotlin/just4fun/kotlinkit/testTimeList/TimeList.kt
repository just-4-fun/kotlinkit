package just4fun.kotlinkit.testTimeList



interface ListElement<E: ListElement<E>>: Comparable<E> {
	val expires: Long
	var next: E?
	override fun compareTo(other: E): Int = expires.compareTo(other.expires)
}


internal object Stub: ListElement<Stub> {
	override val expires: Long = 0
	override var next: Stub? = null
	override fun toString() = "Stub"
}


open class TimeList<E: ListElement<E>> {
	private var head: E? = null
	private var bookmark = Bookmark(@Suppress("UNCHECKED_CAST") (Stub as E), 0, null)
	// metrics
	private var total = 0
	//	private var step = 1
	private var defaultSize = 20//3
	private var minTotal = defaultSize * defaultSize
	private var prevTotal = 0
	private var nextTotal = 0
	private var realSize = 0
	private var minSize = 0
	private var maxSize = 0
	
	init {
		setMetrics(defaultSize)
	}
	
	private fun update(totalValue: Int) {
		total = totalValue
		if (total < minTotal) return
		if (total >= nextTotal) setMetrics(realSize + 10)
		else if (total <= prevTotal) setMetrics(realSize - 10)
	}
	
	private fun recalc(totalValue: Int) {
		total = totalValue
		val newSize = StrictMath.sqrt(total.toDouble()).toInt() + 1
		setMetrics(if (newSize > defaultSize) newSize else defaultSize)
	}
	
	private fun reset() {
		total = 0
		head = null
		bookmark.head = @Suppress("UNCHECKED_CAST") (Stub as E)
		bookmark.next = null
		bookmark.size = 0
		setMetrics(defaultSize)
	}
	
	private fun setMetrics(size: Int) {
		realSize = size
		nextTotal = (size + 1) * (size + 1)
		prevTotal = (size - 1) * (size - 1)
		minSize = realSize - 1// 2
		maxSize = realSize + 1//* 2
		if (total > 0) bookmark.ajustMinSize(null)
	}
	
	
	fun size() = total
	fun isEmpty() = total == 0
	fun nonEmpty() = total > 0
	fun head() = head
	
	fun add(e: E): E {
		update(total + 1)
		if (head == null) {
			head = e
			bookmark.head = e
			bookmark.size = 1
		} else if (e < head!!) {
			e.next = head
			head = e
			bookmark.head = e
			bookmark.size++
		} else bookmark.add(e, null)
		return e
	}
	
	fun remove(): E? {
		if (head == null) return null
		val h = head!!
		if (h.next == null) reset()
		else {
			update(total - 1)
			head = h.next
			bookmark.head = head!!
			bookmark.size--
			h.next = null
			if (bookmark.size < minSize) bookmark.join(null)
		}
		return h
	}
	
	fun remove(e: E): Boolean {
		return if (head == null) false
		else if (e === head) {
			remove(); true
		} else if (bookmark.remove(e, null)) {
			update(total - 1); true
		} else false
	}
	
	fun removeAfter(killTime: Long, apply: (E) -> Unit) {
		if (head == null) return
		if (head!!.expires >= killTime) clear(apply)
		else recalc(bookmark.removeAfter(killTime, null, apply))
	}
	
	fun clear(apply: (E) -> Unit): Unit {
		var curr = head
		while (curr != null) {
			apply(curr)
			val next = curr.next
			curr.next = null
			curr = next
		}
		reset()
	}
	
	fun toList(): List<E> {
		val list = mutableListOf<E>()
		var curr = head
		while (curr != null) {
			list.add(curr)
			curr = curr.next
		}
		return list
	}
	
	fun dump(): String {
		val buf = StringBuilder("$prevTotal<$total<$nextTotal:$realSize  ")
		var boom: Bookmark? = bookmark
		var cur: E? = boom!!.head
		var cur2: E? = boom!!.head
		while (boom != null && cur != null) {
			buf.append("[")
			//			buf.append("[${boom.size}>")
			for (n in 0 until boom.size) {
				if (n > 0) buf.append(",")
				buf.append(cur)
				if (cur !== cur2) buf.append("($cur2)")
				cur = cur?.next
				cur2 = cur2?.next
			}
			buf.append("]")
			boom = boom.next
			cur = boom?.head
			if ((boom == null || cur == null) && cur !== cur2) buf.append("($cur2)")
		}
		return buf.toString()
	}
	
	
	
	
	/* BOOKMARK */
	
	inner class Bookmark(var head: E, var size: Int, var next: Bookmark?) {
		
		fun add(e: E, prevBmk: Bookmark?) {
			val next = next
			if (next != null && next.head <= e) return next.add(e, this)
			//	else
			size++
			val expires = e.expires
			if (size <= maxSize) {
				val cur = unlessNext(head) { it.expires > expires }
				e.next = cur.next; cur.next = e
			} else {
				val mid = size / 2
				var index = 0
				var cur = unlessNext(head, { if (++index == mid) split(it, mid) }) { it.expires > expires }
				e.next = cur.next; cur.next = e
				if (index < mid) {
					while (++index < mid) cur = cur.next!!
					split(cur.next!!, mid)
				}
			}
		}
		
		fun remove(e: E, prevBmk: Bookmark?): Boolean {
			val next = next
			if (next != null && next.head < e) return next.remove(e, this)
			//	else
			val expires = e.expires
			// find beginning of equal 'runTimeNs'
			var cur = unlessNext(head) { it.expires >= expires }
			if (cur.next == null || cur.next!!.expires > expires) return false
			// beginning of equal 'runTimeNs'
			val nextHead = next?.head
			if (nextHead != null && nextHead.expires == expires) {
				// equal 'runTimeNs' continue to next head
				cur = unlessNext(cur) { it === nextHead || it === e }
				if (cur.next === nextHead) return if (nextHead === e) next!!.removeHead(cur, this) else next!!.remove(e, this)
			} else {
				// equal 'runTimeNs' but less than next head
				cur = unlessNext(cur) { it.expires != expires || it === e }
				if (cur.next == null || cur.next!!.expires > expires) return false
			}
			cur.next = e.next
			e.next = null
			size--
			if (size < minSize) join(prevBmk)
			return true
		}
		
		fun removeHead(prev: E, prevBmk: Bookmark): Boolean {
			val newHead = head.next
			prev.next = newHead
			if (newHead == null) prevBmk.next = next
			else {
				head.next = null
				head = newHead
				size--
				if (size < minSize) join(prevBmk)
			}
			return true
		}
		
		fun removeAfter(killTime: Long, prevBmk: Bookmark?, apply: (E) -> Unit): Int {
			val next = next
			if (next != null && next.head.expires < killTime) return size + next.removeAfter(killTime, this, apply)
			//	else
			size = 1
			this.next = null
			val prev = unlessNext(head, { size++ }) { it.expires >= killTime }
			var cur = prev.next
			prev.next = null
			while (cur != null) {
				apply(cur)
				val next = cur.next
				cur.next = null
				cur = next
			}
			return size
		}
		
		inline fun unlessNext(initial: E, exitCond: (next: E) -> Boolean): E {
			var cur = initial
			while (cur.next != null && !exitCond(cur.next!!)) cur = cur.next!!
			return cur
		}
		
		inline fun unlessNext(initial: E, condBody: (next: E) -> Unit, exitCond: (next: E) -> Boolean): E {
			var cur = initial
			while (cur.next != null && !exitCond(cur.next!!)) {
				condBody(cur.next!!)
				cur = cur.next!!
			}
			return cur
		}
		
		fun join(prevBmk: Bookmark?): Bookmark? {
			if (next == null) return null
			val cur = if (prevBmk == null || prevBmk.size > next!!.size) this else prevBmk
			cur.size += cur.next!!.size
			cur.next = cur.next!!.next
			return cur.next
		}
		
		fun split(e: E, index: Int) {
			next = Bookmark(e, size - index, next)
			size = index
		}
		
		fun ajustMinSize(prevBmk: Bookmark?) {
			val nxt = if (size < minSize) join(prevBmk) else next
			nxt?.ajustMinSize(this)
		}
	}
}
