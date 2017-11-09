package kategory.effects.internal

import kategory.effects.Duration
import kategory.Either
import kategory.effects.IO
import kategory.Left
import kategory.Option
import kategory.None
import kategory.Right
import kategory.Some
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.AbstractQueuedSynchronizer

object Platform {

    inline fun <A> onceOnly(crossinline f: (A) -> Unit): (A) -> Unit {
        val wasCalled = AtomicBoolean(false)

        return { a ->
            if (!wasCalled.getAndSet(true)) {
                f(a)

            }
        }
    }

    fun <A> unsafeResync(ioa: IO<A>, limit: Duration): Option<A> {
        val latch = OneShotLatch()
        var ref: Either<Throwable, A>? = null
        ioa.unsafeRunAsync { a ->
            ref = a
            latch.releaseShared(1)
        }

        if (limit == Duration.INFINITE) {
            latch.acquireSharedInterruptibly(1)
        } else {
            latch.tryAcquireSharedNanos(1, limit.nanoseconds)
        }

        val eitherRef = ref

        return when (eitherRef) {
            null -> None
            is Left -> throw eitherRef.a
            is Right -> Some(eitherRef.b)
        }
    }
}

private class OneShotLatch : AbstractQueuedSynchronizer() {
    override fun tryAcquireShared(ignored: Int): Int =
            if (state != 0) {
                1
            } else {
                -1
            }

    override fun tryReleaseShared(ignore: Int): Boolean {
        state = 1
        return true
    }
}

