package ru.kirka.fluxclient.core;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
   private static final Map<Class<? extends Event>, List<EventManager.ListenerData>> registry = new HashMap<>();

   private EventManager() {
   }

   public static void a(Object object) {
      register(object);
   }

   public static void register(Object object) {
      for (Method method : object.getClass().getDeclaredMethods()) {
         if (method.isAnnotationPresent(EventTarget.class) && method.getParameterCount() == 1) {
            registerMethod(method, object);
         }
      }
   }

   public static void b(Object object) {
      unregister(object);
   }

   public static void unregister(Object object) {
      for (List<EventManager.ListenerData> dataList : registry.values()) {
         dataList.removeIf(data -> data.source.equals(object));
      }
   }

   private static void registerMethod(Method method, Object obj) {
      Class<?> param = method.getParameterTypes()[0];
      if (Event.class.isAssignableFrom(param)) {
         method.setAccessible(true);
         byte priority = method.getAnnotation(EventTarget.class).a();
         EventManager.ListenerData data = new EventManager.ListenerData(obj, method, priority);
         registry.computeIfAbsent((Class<? extends Event>)param, k -> new CopyOnWriteArrayList<>()).add(data);
         registry.get(param).sort(Comparator.comparingInt(d -> d.priority));
      }
   }

   public static void a(Event event) {
      post(event);
   }

   public static void post(Event event) {
      List<EventManager.ListenerData> listeners = registry.get(event.getClass());
      if (listeners != null && !listeners.isEmpty()) {
         for (EventManager.ListenerData data : listeners) {
            try {
               data.method.invoke(data.source, event);
            } catch (Exception var5) {
               var5.printStackTrace();
            }
         }
      }
   }

   private static class ListenerData {
      final Object source;
      final Method method;
      final byte priority;

      ListenerData(Object source, Method method, byte priority) {
         this.source = source;
         this.method = method;
         this.priority = priority;
      }
   }
}
