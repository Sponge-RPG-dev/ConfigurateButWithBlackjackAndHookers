# ConfigurateButWithBlackjackAndHookers

Not-so-stupid objectmapper 

This library provides some nice-to have features which default ObjectMapper lacks.

WIP, but works
Examples: 

####Usage ####
The mapper is compatible with ObjectMapper. 

```java

```


---

```java
    @Settings
    @CustomAdapter(MyCustomTypeAdapter.class)
    private String someData;
```
    
    
Unlike default ObjectMapper this one allows you to supply custom (de)serialization logic for one more more specific 
fields within a `@ConfigSerializable`


----

In some cases you might want to get reference to the object which is currently being deserialized within `MyCustomTypeAdapter`. 

```java

@EnableSetterInjection
public class MyCustomTypeAdapter extends TypeSeriializer {
    
    private MyObject root;
    
    @Setter
    public void setRoot(MyObject object) {
        this.root = object;
    }
    .....
}

```


----

```java
    @Settings
    @Static
    public static MY_CONFIG_NODE_I_WANT_TO_EASILY_ACCESS;
```

Unlike default ObjectMapper its allowed to de(serialize) static fields 


Additionally if you are reloading your config its possible to define that the static variable may, or may not be 
updated only once via `@Static(updateable=true/false)`


----

Unlike default ObjectMapper its possible to set collection type. By default ObjectMapper will map only fields as follows:

```java
List<T> -> ArrayList<T>
Set<T> -> HashSet<T>
```

This is not very useful if you from some various reasons want to store your data in a different data structure such TreeSet, SortedList etc.

One might think that you can declared a `@Settings` field already having a desired type impl. However configurate fails to deliver even here.
On top of that writing a code directly against any specific collection impl in java is always seen like a bad practice.

```java
    @Settings
    @AsCollectionImpl(TreeSet.class)
    private Set<T> someSortedSet;
```

As you might already expect `@AsCollectionImpl` will clash with `@CustomAdapter`. If both annotation are present per one specific field `@CustomAdapter` will be prioritized.

Fields annotated with `@AsCollectionImpl` wont ever be null after deserialization. So you can get rid of some null checks as long as you use the annotation.


---

Default ObjectMapper does not support custom handlers of `null`s.
Sometimes you want to ensure that field value is never null even if the node is missing in the config file.  


```java
    @Settings
    @Default(MyObjectDefaultImpl.class)
    private MyObject myobject;
```
