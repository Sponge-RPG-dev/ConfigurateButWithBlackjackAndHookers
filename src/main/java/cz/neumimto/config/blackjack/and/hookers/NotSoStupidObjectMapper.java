package cz.neumimto.config.blackjack.and.hookers;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import cz.neumimto.config.blackjack.and.hookers.annotations.CustomAdapter;
import cz.neumimto.config.blackjack.and.hookers.annotations.Discriminator;
import cz.neumimto.config.blackjack.and.hookers.annotations.Static;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NotSoStupidObjectMapper<T> extends ObjectMapper<T> {


    protected Set<Field> updatedFields = new HashSet<>();
    protected Map<Class<?>, DiscriminatorData> stubs = new HashMap<>();

    public void registerDiscriminatorType(Class<?> iface, Class<?> facade) {
        //Inherited from super classes
        if (type.isAnnotationPresent(Discriminator.class)) {
            Discriminator annotation = type.getAnnotation(Discriminator.class);
        } else {
            TypeToken<?>.TypeSet interfaces = TypeToken.of(type).getTypes().interfaces();
            for (TypeToken<?> anInterface : interfaces) {
                if (anInterface.getRawType().isAnnotationPresent(Discriminator.class)) {
                    Discriminator annotation = type.getAnnotation(Discriminator.class);

                }
            }
        }
    }

    private class DiscriminatorData {
        public String getter;
        public Set<Class<?>> types = new HashSet<>();
    }

    @SuppressWarnings("unchecked")
    public static <T> ObjectMapper<T> forClass(@NonNull Class<T> clazz) throws ObjectMappingException {
        return new NotSoStupidObjectMapper<>(clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> ObjectMapper<T>.BoundInstance forObject(@NonNull T obj) throws ObjectMappingException {
        Preconditions.checkNotNull(obj);
        return forClass((Class<T>) obj.getClass()).bind(obj);
    }

    protected NotSoStupidObjectMapper(Class<T> clazz) throws ObjectMappingException {
        super(clazz);
    }

    private enum StaticFieldPolicy {
        NOT_STATIC,
        ONCE,
        NO_LIMITS
    }

    @Override
    protected void collectFields(Map<String, ObjectMapper.FieldData> cachedFields, Class<? super T> clazz) throws ObjectMappingException {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Setting.class)) {
                Setting setting = field.getAnnotation(Setting.class);
                String path = setting.value();
                if (path.isEmpty()) {
                    path = field.getName();
                }

                StaticFieldPolicy policy = StaticFieldPolicy.NOT_STATIC;
                if (Modifier.isStatic(field.getModifiers()) && field.isAnnotationPresent(Static.class)) {
                    Static annotation = field.getAnnotation(Static.class);
                    policy = annotation.updateable() ? StaticFieldPolicy.NO_LIMITS : StaticFieldPolicy.ONCE;
                }

                TypeSerializer<?> custom = null;
                if (field.isAnnotationPresent(CustomAdapter.class)) {
                    CustomAdapter adapter = field.getAnnotation(CustomAdapter.class);
                    Class<? extends TypeSerializer<?>> value = adapter.value();
                    try {
                        custom = value.getConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new ObjectMappingException("Could not create a new instance from " + value.getCanonicalName()
                                + ". The default constructor is missing, or is not visible", e);
                    }
                }

                FieldData data = new NotSoStupidFieldData(field, setting.comment(), custom, policy);
                field.setAccessible(true);
                if (!cachedFields.containsKey(path)) {
                    cachedFields.put(path, data);
                }
            }
        }
    }


    protected class NotSoStupidFieldData extends ObjectMapper.FieldData {

        private final Field field;
        private final TypeToken<?> fieldType;
        private final TypeSerializer<?> customSerializer;
        private StaticFieldPolicy policy;
        private final String comment;


        public NotSoStupidFieldData(Field field, String comment, TypeSerializer<?> customSerializer, StaticFieldPolicy policy) throws ObjectMappingException {
            super(field, comment);
            this.field = field;
            this.comment = comment;
            this.fieldType = TypeToken.of(field.getGenericType());
            this.customSerializer = customSerializer;
            this.policy = policy;
        }

        public void deserializeFrom(Object instance, ConfigurationNode node) throws ObjectMappingException {
            TypeSerializer<?> serial;
            if (customSerializer != null) {
                serial = customSerializer;
            } else {
                serial = node.getOptions().getSerializers().get(this.fieldType);
                if (serial == null) {
                    throw new ObjectMappingException("No TypeSerializer found for field " + field.getName() + " of type "
                            + this.fieldType);
                }
            }
            Object newVal = node.isVirtual() ? null : serial.deserialize(this.fieldType, node);
            try {
                if (newVal == null) {
                    Object existingVal = null;
                    if (policy == StaticFieldPolicy.NOT_STATIC) {
                        existingVal = field.get(instance);
                    } else {
                        existingVal = field.get(null);
                    }

                    if (existingVal != null) {
                        serializeTo(instance, node);
                    }
                } else {
                    switch (policy) {
                        case ONCE:
                            if (!updatedFields.contains(field)) {
                                field.set(null, newVal);
                                updatedFields.add(field);
                            }
                            break;
                        case NO_LIMITS:
                            field.set(null, newVal);
                            break;
                        case NOT_STATIC:
                            field.set(instance, newVal);
                            break;
                    }

                }
            } catch (IllegalAccessException e) {
                throw new ObjectMappingException("Unable to deserialize field " + field.getName(), e);
            }
        }

        @SuppressWarnings("rawtypes")
        public void serializeTo(Object instance, ConfigurationNode node) throws ObjectMappingException {
            try {
                Object fieldVal = this.field.get(instance);
                if (fieldVal == null) {
                    node.setValue(null);
                } else {
                    TypeSerializer serial = null;
                    if (customSerializer != null) {
                        serial = customSerializer;
                    } else {
                        serial = node.getOptions().getSerializers().get(this.fieldType);
                        if (serial == null) {
                            throw new ObjectMappingException("No TypeSerializer found for field " + field.getName() + " of type " + this.fieldType);
                        }
                    }
                    serial.serialize(this.fieldType, fieldVal, node);
                }

                if (node instanceof CommentedConfigurationNode && this.comment != null && !this.comment.isEmpty()) {
                    CommentedConfigurationNode commentNode = ((CommentedConfigurationNode) node);
                    if (!commentNode.getComment().isPresent()) {
                        commentNode.setComment(this.comment);
                    }
                }
            } catch (IllegalAccessException | ClassCastException e) {
                throw new ObjectMappingException("Unable to serialize field " + field.getName(), e);
            }
        }

        protected String getComment() {
            return comment;
        }

        protected Field getField() {
            return field;
        }

        protected TypeToken<?> getFieldType() {
            return fieldType;
        }
    }
}
