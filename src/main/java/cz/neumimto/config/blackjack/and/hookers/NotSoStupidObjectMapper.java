package cz.neumimto.config.blackjack.and.hookers;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import cz.neumimto.config.blackjack.and.hookers.annotations.AsCollectionImpl;
import cz.neumimto.config.blackjack.and.hookers.annotations.CustomAdapter;
import cz.neumimto.config.blackjack.and.hookers.annotations.Default;
import cz.neumimto.config.blackjack.and.hookers.annotations.Discriminator;
import cz.neumimto.config.blackjack.and.hookers.annotations.EnableSetterInjection;
import cz.neumimto.config.blackjack.and.hookers.annotations.Setter;
import cz.neumimto.config.blackjack.and.hookers.annotations.Static;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMapperFactory;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotSoStupidObjectMapper<T> extends ObjectMapper<T> {

    protected Set<Field> updatedFields = new HashSet<>();
    protected Map<Class<?>, Map<String, Class<?>>> stubs = new HashMap<>();

    private Map<String, ObjectMapper.FieldData> cachedFields;

    static {
        try {
            Field field = TypeSerializers.getDefaultSerializers()
                    .getClass()
                    .getDeclaredField("serializers");
            field.setAccessible(true);
            CopyOnWriteArrayList serializers =(CopyOnWriteArrayList) field.get(TypeSerializers.getDefaultSerializers());
            serializers.remove(3);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        TypeSerializers.getDefaultSerializers()
                .registerPredicate(intput -> intput.getRawType().isAnnotationPresent(ConfigSerializable.class),
                        new ClassTypeNodeSerializer());
    }

    protected NotSoStupidObjectMapper(Class<T> clazz) throws ObjectMappingException {
        super(clazz);
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

    /**
     * dont use this one yet
     */
    public <I, X extends I> void registerDiscriminatorType(Class<? extends I> iface, Class<? extends X> impl) throws ObjectMappingException {
        if (impl.isAnnotationPresent(Discriminator.Value.class)) {
            Discriminator.Value annotation = impl.getAnnotation(Discriminator.Value.class);
            String value = annotation.value();
            Map<String, Class<?>> stringClassMap = stubs.computeIfAbsent(iface, k -> new HashMap<>());
            stringClassMap.put(value, impl);
        }
        throw new ObjectMappingException(impl.getSimpleName() + " is missing @Discriminator.Value");
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


                Class<?> dtype = null;
                /*
                if (stubs.containsKey(type) && field.isAnnotationPresent(Discriminator.class)) {
                    Map<String, Class<?>> stringClassMap = stubs.get(type);
                    Discriminator annotation = field.getAnnotation(Discriminator.class);
                    dtype = stringClassMap.get(annotation.value());
                }
                */
                Class<? extends Collection> collectionImplType = null;
                TypeSerializer<?> custom = null;
                if (field.isAnnotationPresent(CustomAdapter.class) && dtype == null) {
                    CustomAdapter adapter = field.getAnnotation(CustomAdapter.class);
                    Class<? extends TypeSerializer<?>> value = adapter.value();
                    try {
                        custom = value.getConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new ObjectMappingException("Could not create a new instance from " + value.getCanonicalName()
                                + ". The default constructor is missing, or is not visible", e);
                    }
                } else if (dtype == null && field.isAnnotationPresent(AsCollectionImpl.class)) {
                    collectionImplType = field.getAnnotation(AsCollectionImpl.class).value();
                }

                FieldData data = new NotSoStupidFieldData(field, setting.comment(), custom, policy, dtype, collectionImplType);
                field.setAccessible(true);
                if (!cachedFields.containsKey(path)) {
                    cachedFields.put(path, data);
                }
            }
        }
        this.cachedFields = cachedFields;
    }

    public enum StaticFieldPolicy {
        NOT_STATIC,
        ONCE,
        NO_LIMITS
    }

    protected class NotSoStupidFieldData extends ObjectMapper.FieldData {

        private final Field field;
        private final TypeToken<?> fieldType;
        private final TypeSerializer<?> customSerializer;
        private final String comment;
        private StaticFieldPolicy policy;
        private final Class<? extends Collection> collectionImplType;
        private TypeToken dtype;


        public NotSoStupidFieldData(Field field, String comment, TypeSerializer<?> customSerializer, StaticFieldPolicy policy,
                                    Class<?> dtype, Class<? extends Collection> collectionImplType)
                throws ObjectMappingException {
            super(field, comment);
            this.field = field;
            this.comment = comment;
            this.fieldType = TypeToken.of(field.getGenericType());
            this.customSerializer = customSerializer;
            this.policy = policy;
            // this.dtype = TypeToken.of(dtype);
            this.collectionImplType = collectionImplType;
        }

        public void deserializeFrom(Object instance, ConfigurationNode node) throws ObjectMappingException {
            TypeSerializer<?> serial;
            node.getOptions().setObjectMapperFactory(new ObjectMapperFactory() {
                @Override
                public @NonNull <T> ObjectMapper<T> getMapper(@NonNull Class<T> type) throws ObjectMappingException {
                    return new NotSoStupidObjectMapper<>(type);
                }
            });
            if (customSerializer != null) {
                serial = customSerializer;
            } else {
                if (dtype != null) {
                    serial = node.getOptions().getSerializers().get(dtype);
                } else {
                    serial = node.getOptions().getSerializers().get(this.fieldType);
                }
                if (serial == null) {
                    throw new ObjectMappingException("No TypeSerializer found for field " + field.getName() + " of type "
                            + this.fieldType);
                }
            }
            Class<?> aClass = serial.getClass();
            if (aClass.isAnnotationPresent(EnableSetterInjection.class)) {
                for (Method declaredMethod : aClass.getDeclaredMethods()) {
                    if (declaredMethod.isAnnotationPresent(Setter.class)) {
                        try {
                            declaredMethod.invoke(serial, instance);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new ObjectMappingException("Unable to invoker @setter method on " + serial.getClass().getSimpleName(), e);
                        }
                        break;
                    }
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

                    if (existingVal == null && collectionImplType != null) {
                        try {
                            existingVal = collectionImplType.getConstructor().newInstance();
                            field.set(instance, existingVal);
                        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException e) {
                            throw new ObjectMappingException("Collection interface implementation " + collectionImplType + " is missing default ctr.");
                        }
                    }

                    if (existingVal == null && field.isAnnotationPresent(Default.class)) {
                        existingVal = field.getAnnotation(Default.class).value().getConstructor().newInstance();
                    }

                    if (existingVal != null) {
                        serializeTo(instance, node);
                    }
                } else {


                    if (collectionImplType != null) {
                        try {
                            Collection collection = collectionImplType.getConstructor().newInstance();
                            collection.addAll((Collection) newVal);
                            newVal = collection;
                        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException e) {
                            throw new ObjectMappingException("Collection interface implementation " + collectionImplType + " is missing default ctr.");
                        }
                    }

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
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
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

    @Override
    public BoundInstance bind(T instance) {
        return new NSSBoundInstance(instance);
    }

    @Override
    public BoundInstance bindToNew() throws ObjectMappingException {
        return new NSSBoundInstance(constructObject());
    }

    public class NSSBoundInstance extends BoundInstance {
        private final T t;

        protected NSSBoundInstance(T t) {
            super(t);
            this.t = t;
        }

        public T populate(ConfigurationNode source) throws ObjectMappingException {
            for (Map.Entry<String, FieldData> ent : cachedFields.entrySet()) {
                ConfigurationNode node = source.getNode(ent.getKey());
                node.getOptions().setObjectMapperFactory(NotSoStupidObjectMapper::new);
                ent.getValue().deserializeFrom(t, node);
            }
            return t;
        }

        /**
         * Serialize the data contained in annotated fields to the configuration node.
         *
         * @param target The target node to serialize to
         * @throws ObjectMappingException if serialization was not possible due to some error.
         */
        public void serialize(ConfigurationNode target) throws ObjectMappingException {
            for (Map.Entry<String, FieldData> ent : cachedFields.entrySet()) {
                ConfigurationNode node = target.getNode(ent.getKey());
                node.getOptions().setObjectMapperFactory(NotSoStupidObjectMapper::new);
                ent.getValue().serializeTo(t, node);
            }
        }

        /**
         * Return the instance this mapper is bound to.
         *
         * @return The active instance
         */
        public T getInstance() {
            return t;
        }

    }
}
