package neverlose.events.api;

import neverlose.events.api.events.Event;
import neverlose.events.api.events.EventStoppable;
import neverlose.events.api.types.Priority;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventManager {
    private static final Map<Class<? extends Event>, List<MethodData>> REGISTRY_MAP = new HashMap<>();
    private static final Map<String, Long> ERROR_THROTTLE = new ConcurrentHashMap<>();
    private static final long ERROR_THROTTLE_MS = 2_500L;

    public EventManager() {}

    public static void register(Object object) {
        for (final Method method : object.getClass().getDeclaredMethods()) {
            if (isMethodBad(method)) {
                continue;
            }

            register(method, object);
        }
    }

    public static void register(Object object, Class<? extends Event> eventClass) {
        for (final Method method : object.getClass().getDeclaredMethods()) {
            if (isMethodBad(method, eventClass)) {
                continue;
            }

            register(method, object);
        }
    }

    public static void unregister(Object object) {
        for (final List<MethodData> dataList : REGISTRY_MAP.values()) {
            dataList.removeIf(data -> data.source().equals(object));
        }

        cleanMap(true);
    }

    public static void unregister(Object object, Class<? extends Event> eventClass) {
        if (REGISTRY_MAP.containsKey(eventClass)) {
            REGISTRY_MAP.get(eventClass).removeIf(data -> data.source().equals(object));
            cleanMap(true);
        }
    }

    private static void register(Method method, Object object) {
        @SuppressWarnings("unchecked")
        Class<? extends Event> indexClass = (Class<? extends Event>) method.getParameterTypes()[0];
        final MethodData data = new MethodData(object, method, method.getAnnotation(EventHandler.class).value());

        if (!data.target().canAccess(data.source())) {
            data.target().setAccessible(true);
        }

        if (REGISTRY_MAP.containsKey(indexClass)) {
            if (!REGISTRY_MAP.get(indexClass).contains(data)) {
                REGISTRY_MAP.get(indexClass).add(data);
                sortListValue(indexClass);
            }
        } else {
            REGISTRY_MAP.put(indexClass, new CopyOnWriteArrayList<>());
            REGISTRY_MAP.get(indexClass).add(data);
        }
    }

    public static void removeEntry(Class<? extends Event> indexClass) {
        REGISTRY_MAP.entrySet().removeIf(entry -> entry.getKey().equals(indexClass));
    }

    public static void cleanMap(boolean onlyEmptyEntries) {
        if (onlyEmptyEntries) {
            REGISTRY_MAP.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        } else {
            REGISTRY_MAP.clear();
        }
    }

    private static void sortListValue(Class<? extends Event> indexClass) {
        List<MethodData> sortedList = new CopyOnWriteArrayList<>();

        for (final byte priority : Priority.VALUE_ARRAY) {
            for (final MethodData data : REGISTRY_MAP.get(indexClass)) {
                if (data.priority() == priority) {
                    sortedList.add(data);
                }
            }
        }

        REGISTRY_MAP.put(indexClass, sortedList);
    }

    private static boolean isMethodBad(Method method) {
        return method.getParameterTypes().length != 1 || !method.isAnnotationPresent(EventHandler.class);
    }

    private static boolean isMethodBad(Method method, Class<? extends Event> eventClass) {
        return isMethodBad(method) || !method.getParameterTypes()[0].equals(eventClass);
    }

    public static Event callEvent(final Event event) {
        List<MethodData> dataList = REGISTRY_MAP.get(event.getClass());

        if (dataList != null) {
            if (event instanceof EventStoppable stoppable) {
                for (final MethodData data : dataList) {
                    invoke(data, event);

                    if (stoppable.isStopped()) {
                        break;
                    }
                }
            } else {
                for (final MethodData data : dataList) {
                    invoke(data, event);
                }
            }
        }

        return event;
    }

    private static void invoke(MethodData data, Event argument) {
        try {
            data.target().invoke(data.source(), argument);
        } catch (IllegalAccessException e) {
            logInvocationError(data, argument, e);
        } catch (IllegalArgumentException e) {
            logInvocationError(data, argument, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            logInvocationError(data, argument, cause);
        }
    }

    private static void logInvocationError(MethodData data, Event argument, Throwable throwable) {
        String key = data.source().getClass().getName() + "#" + data.target().getName() + ":" + throwable.getClass().getName();
        long now = System.currentTimeMillis();
        Long last = ERROR_THROTTLE.get(key);

        if (last != null && now - last < ERROR_THROTTLE_MS) {
            return;
        }

        ERROR_THROTTLE.put(key, now);

        String message = "[" + now + "] Event handler failed: "
                + data.source().getClass().getName()
                + "#" + data.target().getName()
                + "(" + argument.getClass().getSimpleName() + ") -> "
                + throwable.getClass().getName()
                + ": " + String.valueOf(throwable.getMessage());

        try {
            Files.writeString(
                    Path.of("Neverlose-event-errors.log"),
                    message + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {
        }
    }

    private record MethodData(Object source, Method target, byte priority) {}
}
