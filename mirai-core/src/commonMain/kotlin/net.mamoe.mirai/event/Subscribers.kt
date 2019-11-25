@file:Suppress("unused")

package net.mamoe.mirai.event

import net.mamoe.mirai.event.internal.Handler
import net.mamoe.mirai.event.internal.Listener
import net.mamoe.mirai.event.internal.subscribeInternal
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

/*
 * 该文件为所有的订阅事件的方法.
 */

/**
 * 订阅者的状态
 */
inline class ListeningStatus(inline val listening: Boolean) {
    companion object {
        @JvmStatic
        val LISTENING = ListeningStatus(true)
        @JvmStatic
        val STOPPED = ListeningStatus(false)
    }
}


// region 顶层方法

/**
 * 订阅所有 [E] 及其子类事件.
 * 在
 */
suspend inline fun <reified E : Subscribable> subscribe(noinline handler: suspend (E) -> ListeningStatus) = E::class.subscribe(handler)

suspend inline fun <reified E : Subscribable> subscribeAlways(noinline listener: suspend (E) -> Unit) = E::class.subscribeAlways(listener)

suspend inline fun <reified E : Subscribable> subscribeOnce(noinline listener: suspend (E) -> Unit) = E::class.subscribeOnce(listener)

suspend inline fun <reified E : Subscribable, T> subscribeUntil(valueIfStop: T, noinline listener: suspend (E) -> T) =
    E::class.subscribeUntil(valueIfStop, listener)

suspend inline fun <reified E : Subscribable> subscribeUntilFalse(noinline listener: suspend (E) -> Boolean) = E::class.subscribeUntilFalse(listener)
suspend inline fun <reified E : Subscribable> subscribeUntilTrue(noinline listener: suspend (E) -> Boolean) = E::class.subscribeUntilTrue(listener)
suspend inline fun <reified E : Subscribable> subscribeUntilNull(noinline listener: suspend (E) -> Any?) = E::class.subscribeUntilNull(listener)


suspend inline fun <reified E : Subscribable, T> subscribeWhile(valueIfContinue: T, noinline listener: suspend (E) -> T) =
    E::class.subscribeWhile(valueIfContinue, listener)

suspend inline fun <reified E : Subscribable> subscribeWhileFalse(noinline listener: suspend (E) -> Boolean) = E::class.subscribeWhileFalse(listener)
suspend inline fun <reified E : Subscribable> subscribeWhileTrue(noinline listener: suspend (E) -> Boolean) = E::class.subscribeWhileTrue(listener)
suspend inline fun <reified E : Subscribable> subscribeWhileNull(noinline listener: suspend (E) -> Any?) = E::class.subscribeWhileNull(listener)

// endregion


// region KClass 的扩展方法 (不推荐)

@PublishedApi
internal suspend fun <E : Subscribable> KClass<E>.subscribe(handler: suspend (E) -> ListeningStatus) = this.subscribeInternal(Handler(handler))

@PublishedApi
internal suspend fun <E : Subscribable> KClass<E>.subscribeAlways(listener: suspend (E) -> Unit) =
    this.subscribeInternal(Handler { listener(it); ListeningStatus.LISTENING })

@PublishedApi
internal suspend fun <E : Subscribable> KClass<E>.subscribeOnce(listener: suspend (E) -> Unit) =
    this.subscribeInternal(Handler { listener(it); ListeningStatus.STOPPED })

@PublishedApi
internal suspend fun <E : Subscribable, T> KClass<E>.subscribeUntil(valueIfStop: T, listener: suspend (E) -> T) =
    subscribeInternal(Handler { if (listener(it) === valueIfStop) ListeningStatus.STOPPED else ListeningStatus.LISTENING })

@PublishedApi
internal suspend fun <E : Subscribable> KClass<E>.subscribeUntilFalse(listener: suspend (E) -> Boolean) = subscribeUntil(false, listener)

@PublishedApi
internal suspend fun <E : Subscribable> KClass<E>.subscribeUntilTrue(listener: suspend (E) -> Boolean) = subscribeUntil(true, listener)

@PublishedApi
internal suspend fun <E : Subscribable> KClass<E>.subscribeUntilNull(listener: suspend (E) -> Any?) = subscribeUntil(null, listener)


@PublishedApi
internal suspend fun <E : Subscribable, T> KClass<E>.subscribeWhile(valueIfContinue: T, listener: suspend (E) -> T) =
    subscribeInternal(Handler { if (listener(it) !== valueIfContinue) ListeningStatus.STOPPED else ListeningStatus.LISTENING })

@PublishedApi
internal suspend fun <E : Subscribable> KClass<E>.subscribeWhileFalse(listener: suspend (E) -> Boolean) = subscribeWhile(false, listener)

@PublishedApi
internal suspend fun <E : Subscribable> KClass<E>.subscribeWhileTrue(listener: suspend (E) -> Boolean) = subscribeWhile(true, listener)

@PublishedApi
internal suspend fun <E : Subscribable> KClass<E>.subscribeWhileNull(listener: suspend (E) -> Any?) = subscribeWhile(null, listener)

// endregion

// region ListenerBuilder DSL

/**
 * 监听一个事件. 可同时进行多种方式的监听
 * @see ListenerBuilder
 */
@ListenersBuilderDsl
@PublishedApi
internal suspend fun <E : Subscribable> KClass<E>.subscribeAll(listeners: suspend ListenerBuilder<E>.() -> Unit) {
    with(ListenerBuilder<E> { this.subscribeInternal(it) }) {
        listeners()
    }
}

/**
 * 监听一个事件. 可同时进行多种方式的监听
 * @see ListenerBuilder
 */
@ListenersBuilderDsl
suspend inline fun <reified E : Subscribable> subscribeAll(noinline listeners: suspend ListenerBuilder<E>.() -> Unit) = E::class.subscribeAll(listeners)

/**
 * 监听构建器. 可同时进行多种方式的监听
 *
 * ```kotlin
 * FriendMessageEvent.subscribe {
 *   always{
 *     it.reply("永远发生")
 *   }
 *
 *   untilFalse {
 *     it.reply("你发送了 ${it.event}")
 *     it.event eq "停止"
 *   }
 * }
 * ```
 */
@ListenersBuilderDsl
@Suppress("MemberVisibilityCanBePrivate", "unused")
inline class ListenerBuilder<out E : Subscribable>(
    @PublishedApi internal inline val handlerConsumer: suspend (Listener<E>) -> Unit
) {
    suspend inline fun handler(noinline listener: suspend (E) -> ListeningStatus) {
        handlerConsumer(Handler(listener))
    }

    suspend inline fun always(noinline listener: suspend (E) -> Unit) = handler { listener(it); ListeningStatus.LISTENING }

    suspend inline fun <T> until(until: T, noinline listener: suspend (E) -> T) =
        handler { if (listener(it) === until) ListeningStatus.STOPPED else ListeningStatus.LISTENING }

    suspend inline fun untilFalse(noinline listener: suspend (E) -> Boolean) = until(false, listener)
    suspend inline fun untilTrue(noinline listener: suspend (E) -> Boolean) = until(true, listener)
    suspend inline fun untilNull(noinline listener: suspend (E) -> Any?) = until(null, listener)


    suspend inline fun <T> `while`(until: T, noinline listener: suspend (E) -> T) =
        handler { if (listener(it) !== until) ListeningStatus.STOPPED else ListeningStatus.LISTENING }

    suspend inline fun whileFalse(noinline listener: suspend (E) -> Boolean) = `while`(false, listener)
    suspend inline fun whileTrue(noinline listener: suspend (E) -> Boolean) = `while`(true, listener)
    suspend inline fun whileNull(noinline listener: suspend (E) -> Any?) = `while`(null, listener)


    suspend inline fun once(noinline listener: suspend (E) -> Unit) = handler { listener(it); ListeningStatus.STOPPED }
}

@DslMarker
annotation class ListenersBuilderDsl

// endregion