# ConfigurateButWithBlackjackAndHookers

Not so stupid object mapper 

WIP, might now work yet

Examples: 

  
    @Settings
    @CustomAdapter(MyCustomTypeAdapter.class)
    private String someData;
    
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

    @Settings
    @Static
    public static MY_CONFIG_NODE_I_WANT_TO_EASILY_ACCESS;
    
Unlike default ObjectMapper its allowed to de(serialize) static fields 


Additionally if you are reloading your config its possible to define that the static variable may, or may not be 
updated only once via `@Static(updateable=true/false)`
