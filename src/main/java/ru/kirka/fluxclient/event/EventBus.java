package ru.kirka.fluxclient.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventBus {
   private final Map<Class<? extends Event>, List<EventBus.Invoker>> registry = new ConcurrentHashMap<>();

   public void register(Object subscriber) {
      if (subscriber != null) {
         for (Method method : subscriber.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventBus.Subscribe.class) && method.getParameterCount() == 1) {
               Class<?> paramType = method.getParameterTypes()[0];
               if (Event.class.isAssignableFrom(paramType)) {
                  method.setAccessible(true);
                  int priority = method.getAnnotation(EventBus.Subscribe.class).priority();
                  this.addInvoker((Class<? extends Event>)paramType, new EventBus.MethodInvoker(subscriber, method, priority));
               }
            }
         }

         for (Class<?> current = subscriber.getClass(); current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
               if (EventListener.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType pt) {
                  Type[] args = pt.getActualTypeArguments();
                  if (args.length > 0 && args[0] instanceof Class<?> eventClass && Event.class.isAssignableFrom(eventClass)) {
                     try {
                        field.setAccessible(true);
                        EventListener<?> listener = (EventListener<?>)field.get(subscriber);
                        if (listener != null) {
                           this.addInvoker((Class<? extends Event>)eventClass, new EventBus.FieldInvoker(subscriber, listener));
                        }
                     } catch (IllegalAccessException var13) {
                        var13.printStackTrace();
                     }
                  }
               }
            }
         }
      }
   }

   private void addInvoker(Class<? extends Event> eventType, EventBus.Invoker invoker) {
      List<EventBus.Invoker> list = this.registry.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
      list.add(invoker);
      list.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
   }

   public void unregister(Object subscriber) {
      if (subscriber != null) {
         this.registry.values().forEach(list -> list.removeIf(inv -> inv.target() == subscriber));
      }
   }

   public <T extends Event> T post(T event) {
      List<EventBus.Invoker> invokers = this.registry.get(event.getClass());
      if (invokers != null && !invokers.isEmpty()) {
         for (EventBus.Invoker invoker : invokers) {
            try {
               invoker.invoke(event);
            } catch (Throwable var6) {
               var6.printStackTrace();
            }
         }
      }

      return event;
   }

   private record FieldInvoker(Object target, EventListener listener) implements EventBus.Invoker {
      @Override
      public int priority() {
         return this.listener.getPriority();
      }

      @Override
      public void invoke(Event event) {
         this.listener.onEvent(event);
      }
   }

   private interface Invoker {
      Object target();

      int priority();

      void invoke(Event var1) throws Throwable;
   }

   private record MethodInvoker(Object target, Method method, int priority) implements EventBus.Invoker {
      @Override
      public void invoke(Event event) throws Throwable {
         this.method.invoke(this.target, event);
      }
   }

   @Retention(RetentionPolicy.RUNTIME)
   @Target(ElementType.METHOD)
   public @interface Subscribe {
      int priority() default 0;
   }
}
