# ConfigurateButWithBlackjackAndHookers

Not so stupid object mapper 

WIP, but works
Examples: 

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

As you might already expect `@AsCollectionImpl` will clash with `@CustomAdapter`
