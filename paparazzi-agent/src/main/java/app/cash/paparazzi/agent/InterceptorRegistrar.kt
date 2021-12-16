package app.cash.paparazzi.agent

import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers

object InterceptorRegistrar {
  private val byteBuddy = ByteBuddy()
  private val methodInterceptors = mutableListOf<() -> Unit>()

  fun addMethodInterceptor(
    receiver: Class<*>,
    methodName: String,
    interceptor: Class<*>,
    arguments: List<Class<*>> = emptyList()
  ) = addMethodInterceptors(receiver, setOf(methodName to interceptor), arguments)

  fun addMethodInterceptors(
    receiver: Class<*>,
    methodNamesToInterceptors: Set<Pair<String, Class<*>>>,
    arguments:List<Class<*>> = emptyList()
  ) {
    val junctionToInterceptors = methodNamesToInterceptors.map {
      var named = ElementMatchers.named<MethodDescription?>(it.first)
      arguments.forEachIndexed { index, clazz ->
        named = named.and(ElementMatchers.takesArgument(index, clazz))
      }
       named to it.second
    }
    methodInterceptors += {
      var builder = byteBuddy
        .redefine(receiver)

      junctionToInterceptors.forEach {
        builder = builder
          .method(it.first)
          .intercept(MethodDelegation.to(it.second))
      }

      builder
        .make()
        .load(receiver.classLoader, ClassReloadingStrategy.fromInstalledAgent())
    }
  }

  fun registerMethodInterceptors() {
    methodInterceptors.forEach { it.invoke() }
  }

  fun clearMethodInterceptors() {
    methodInterceptors.clear()
  }
}