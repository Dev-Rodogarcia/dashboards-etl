package com.dashboard.api.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

final class TestReflectionUtils {

    private TestReflectionUtils() {
    }

    static <T> T novaInstancia(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Nao foi possivel instanciar " + type.getSimpleName(), ex);
        }
    }

    static void setField(Object target, String nomeCampo, Object valor) {
        try {
            Field field = target.getClass().getDeclaredField(nomeCampo);
            field.setAccessible(true);
            field.set(target, valor);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Nao foi possivel definir o campo " + nomeCampo + ".", ex);
        }
    }
}
