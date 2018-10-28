


PlatformTransactionManager 
 TransactionException that can be thrown by any of the PlatformTransactionManager interface’s methods is unchecked
 
The getTransaction(..) method returns a TransactionStatus object, depending on a TransactionDefinition parameter. The returned TransactionStatus might represent a new transaction or can represent an existing transaction, if a matching transaction exists in the current call stack. The implication in this latter case is that, as with Java EE transaction contexts, a TransactionStatus is associated with a thread of execution.

The TransactionDefinition interface specifies:

Propagation: Typically, all code executed within a transaction scope runs in that transaction. However, you can specify the behavior if a transactional method is executed when a transaction context already exists. For example, code can continue running in the existing transaction (the common case), or the existing transaction can be suspended and a new transaction created. Spring offers all of the transaction propagation options familiar from EJB CMT. To read about the semantics of transaction propagation in Spring, see Transaction Propagation.

Isolation: The degree to which this transaction is isolated from the work of other transactions. For example, can this transaction see uncommitted writes from other transactions?

Timeout: How long this transaction runs before timing out and being automatically rolled back by the underlying transaction infrastructure.

Read-only status: You can use a read-only transaction when your code reads but does not modify data. Read-only transactions can be a useful optimization in some cases, such as when you use Hibernate.


In the case of JDBC, instead of the traditional JDBC approach of calling the getConnection() method on the DataSource, you can instead use Spring’s org.springframework.jdbc.datasource.DataSourceUtils class, as follows:
Connection conn = DataSourceUtils.getConnection(dataSource);


Any SQLException is wrapped in a Spring Framework CannotGetJdbcConnectionException, one of the Spring Framework’s hierarchy of unchecked DataAccessException types. This approach gives you more information than can be obtained easily from the SQLException and ensures portability across databases and even across different persistence technologies.

Once you have used Spring’s JDBC support, JPA support, or Hibernate support, you generally prefer not to use DataSourceUtils or the other helper classes, because you are much happier working through the Spring abstraction than directly with the relevant APIs. For example, if you use the Spring JdbcTemplate or jdbc.object package to simplify your use of JDBC, correct connection retrieval occurs behind the scenes and you need not write any special code.

You can make a setRollbackOnly() call within a transaction context, if necessary.Although you can still call setRollbackOnly() on the TransactionStatus object to roll back the current transaction back, most often you can specify a rule that MyApplicationException must always result in rollback. The significant advantage to this option is that business objects do not depend on the transaction infrastructure. For example, they typically do not need to import Spring transaction APIs or other Spring APIs.

Spring default behavior for declarative transaction management follows EJB convention (roll back is automatic only on unchecked exceptions), 

It is not sufficient merely to tell you to annotate your classes with the @Transactional annotation, add @EnableTransactionManagement to your configuration, and expect you to understand how it all works

The combination of AOP with transactional metadata yields an AOP proxy that uses a TransactionInterceptor in conjunction with an appropriate PlatformTransactionManager implementation to drive transactions around method invocations.

If you do not want a transaction rolled back when an exception is thrown, you can also specify 'no rollback rules'. The following example tells the Spring Framework’s transaction infrastructure to commit the attendant transaction even in the face of an unhandled InstrumentNotFoundException:

<tx:advice id="txAdvice">
    <tx:attributes>
    <tx:method name="updateStock" no-rollback-for="InstrumentNotFoundException"/>
    <tx:method name="*"/>
    </tx:attributes>
</tx:advice>
The configuration shown earlier is used to create a transactional proxy around the object that is created from the fooService bean definition. The proxy is configured with the transactional advice so that, when an appropriate method is invoked on the proxy, a transaction is started, suspended, marked as read-only, and so on, depending on the transaction configuration associated with that method. 

When the Spring Framework’s transaction infrastructure catches an exception and it consults the configured rollback rules to determine whether to mark the transaction for rollback, the strongest matching rule wins. So, in the case of the following configuration, any exception other than an InstrumentNotFoundException results in a rollback of the attendant transaction:

<tx:advice id="txAdvice">
    <tx:attributes>
    <tx:method name="*" rollback-for="Throwable" no-rollback-for="InstrumentNotFoundException"/>
    </tx:attributes>
</tx:advice>


The default <tx:advice/> settings are:
The propagation setting is REQUIRED.
The isolation level is DEFAULT.
The transaction is read-write.
The transaction timeout defaults to the default timeout of the underlying transaction system or none if timeouts are not supported.
Any RuntimeException triggers rollback, and any checked Exception does not.

You can omit the transaction-manager attribute in the <tx:annotation-driven/> tag if the bean name of the PlatformTransactionManager that you want to wire in has the name, transactionManager. If the PlatformTransactionManager bean that you want to dependency-inject has any other name, you have to use the transaction-manager attribute


When you use proxies, you should apply the @Transactional annotation only to methods with public visibility. If you do annotate protected, private or package-visible methods with the @Transactional annotation, no error is raised, but the annotated method does not exhibit the configured transactional settings. If you need to annotate non-public methods, consider using AspectJ (described later).

The Spring team recommends that you annotate only concrete classes (and methods of concrete classes) with the @Transactional annotation, as opposed to annotating interfaces. You certainly can place the @Transactional annotation on an interface (or an interface method), but this works only as you would expect it to if you use interface-based proxies. The fact that Java annotations are not inherited from interfaces means that, if you use class-based proxies (proxy-target-class="true") or the weaving-based aspect (mode="aspectj"), the transaction settings are not recognized by the proxying and weaving infrastructure, and the object is not wrapped in a transactional proxy, which would be decidedly bad.

In proxy mode (which is the default), only external method calls coming in through the proxy are intercepted. This means that self-invocation (in effect, a method within the target object calling another method of the target object) does not lead to an actual transaction at runtime even if the invoked method is marked with @Transactional. Also, the proxy must be fully initialized to provide the expected behavior, so you should not rely on this feature in your initialization code (that is, @PostConstruct).

Consider using of AspectJ mode (see the mode attribute in the following table) if you expect self-invocations to be wrapped with transactions as well. In this case, there no proxy in the first place. Instead, the target class is woven (that is, its byte code is modified) to turn @Transactional into runtime behavior on any kind of method.

mode

mode

proxy

The default mode (proxy) processes annotated beans to be proxied by using Spring’s AOP framework (following proxy semantics, as discussed earlier, applying to method calls coming in through the proxy only). The alternative mode (aspectj) instead weaves the affected classes with Spring’s AspectJ transaction aspect, modifying the target class byte code to apply to any kind of method call. AspectJ weaving requires spring-aspects.jar in the classpath as well as having load-time weaving (or compile-time weaving) enabled. (See Spring configuration for details on how to set up load-time weaving.)

proxy-target-class

proxyTargetClass

false

Applies to proxy mode only. Controls what type of transactional proxies are created for classes annotated with the @Transactional annotation. If the proxy-target-class attribute is set to true, class-based proxies are created. If proxy-target-class is false or if the attribute is omitted, then standard JDK interface-based proxies are created. (See Proxying mechanisms for a detailed examination of the different proxy types.)

@EnableTransactionManagement and <tx:annotation-driven/> looks for @Transactional only on beans in the same application context in which they are defined. This means that, if you put annotation-driven configuration in a WebApplicationContext for a DispatcherServlet, it checks for @Transactional beans only in your controllers and not your services

Most Spring applications need only a single transaction manager, but there may be situations where you want multiple independent transaction managers in a single application. You can use the value attribute of the @Transactional annotation to optionally specify the identity of the PlatformTransactionManager to be used. This can either be the bean name or the qualifier value of the transaction manager bean. For example, using the qualifier notation, you can combine the following Java code with the following transaction manager bean declarations in the application context:

public class TransactionalService {

    @Transactional("order")
    public void setSomething(String name) { ... }

    @Transactional("account")
    public void doSomething() { ... }
}
The following listing shows the bean declarations:

<tx:annotation-driven/>

    <bean id="transactionManager1" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        ...
        <qualifier value="order"/>
    </bean>

    <bean id="transactionManager2" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        ...
        <qualifier value="account"/>
    </bean>
    
    If you find you repeatedly use the same attributes with @Transactional on many different methods, Spring’s meta-annotation support lets you define custom shortcut annotations for your specific use cases. For example, consider the following annotation definitions:

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Transactional("order")
public @interface OrderTx {
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Transactional("account")
public @interface AccountTx {
}
The preceding annotations lets us write the example from the previous section as follows:

public class TransactionalService {

    @OrderTx
    public void setSomething(String name) { ... }

    @AccountTx
    public void doSomething() { ... }
}

	requires_new
PROPAGATION_REQUIRES_NEW, in contrast to PROPAGATION_REQUIRED, always uses an independent physical transaction for each affected transaction scope, never participating in an existing transaction for an outer scope. In such an arrangement, the underlying resource transactions are different and, hence, can commit or roll back independently, with an outer transaction not affected by an inner transaction’s rollback status and with an inner transaction’s locks released immediately after its completion. Such an independent inner transaction can also declare its own isolation level, timeout, and read-only settings and not inherit an outer transaction’s characteristics.

	nested
PROPAGATION_NESTED uses a single physical transaction with multiple savepoints that it can roll back to. Such partial rollbacks let an inner transaction scope trigger a rollback for its scope, with the outer transaction being able to continue the physical transaction despite some operations having been rolled back. This setting is typically mapped onto JDBC savepoints, so it works only with JDBC resource transactions.


	Programmatic  Transaction Management
The Spring Framework provides two means of programmatic transaction management, by using:

The TransactionTemplate.

A PlatformTransactionManager implementation directly.

The Spring team generally recommends the TransactionTemplate for programmatic transaction management. The second approach is similar to using the JTA UserTransaction API, although exception handling is less cumbersome

	Using the TransactionTemplate
The TransactionTemplate adopts the same approach as other Spring templates, such as the JdbcTemplate. It uses a callback approach (to free application code from having to do the boilerplate acquisition and release transactional resources) and results in code that is intention driven, in that your code focuses solely on what you want to do.

As the examples that follow show, using the TransactionTemplate absolutely couples you to Spring’s transaction infrastructure and APIs. Whether or not programmatic transaction management is suitable for your development needs is a decision that you have to make yourself.
Application code that must execute in a transactional context and that explicitly uses the TransactionTemplate resembles the next example. You, as an application developer, can write a TransactionCallback implementation (typically expressed as an anonymous inner class) that contains the code that you need to execute in the context of a transaction. You can then pass an instance of your custom TransactionCallback to the execute(..) method exposed on the TransactionTemplate. The following example shows how to do so:

public class SimpleService implements Service {

    // single TransactionTemplate shared amongst all methods in this instance
    private final TransactionTemplate transactionTemplate;

    // use constructor-injection to supply the PlatformTransactionManager
    public SimpleService(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public Object someServiceMethod() {
        return transactionTemplate.execute(new TransactionCallback() {
            // the code in this method executes in a transactional context
            public Object doInTransaction(TransactionStatus status) {
                updateOperation1();
                return resultOfUpdateOperation2();
            }
        });
    }
}
If there is no return value, you can use the convenient TransactionCallbackWithoutResult class with an anonymous class, as follows:

transactionTemplate.execute(new TransactionCallbackWithoutResult() {
    protected void doInTransactionWithoutResult(TransactionStatus status) {
        updateOperation1();
        updateOperation2();
    }
});
Code within the callback can roll the transaction back by calling the setRollbackOnly() method on the supplied TransactionStatus object, as follows:

transactionTemplate.execute(new TransactionCallbackWithoutResult() {

    protected void doInTransactionWithoutResult(TransactionStatus status) {
        try {
            updateOperation1();
            updateOperation2();
        } catch (SomeBusinessException ex) {
            status.setRollbackOnly();
        }
    }
});
Specifying Transaction Settings
You can specify transaction settings (such as the propagation mode, the isolation level, the timeout, and so forth) on the TransactionTemplate either programmatically or in configuration. By default, TransactionTemplate instances have the default transactional settings. The following example shows the programmatic customization of the transactional settings for a specific TransactionTemplate:

public class SimpleService implements Service {

    private final TransactionTemplate transactionTemplate;

    public SimpleService(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);

        // the transaction settings can be set here explicitly if so desired
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
        this.transactionTemplate.setTimeout(30); // 30 seconds
        // and so forth...
    }
}
The following example defines a TransactionTemplate with some custom transactional settings by using Spring XML configuration:

<bean id="sharedTransactionTemplate"
        class="org.springframework.transaction.support.TransactionTemplate">
    <property name="isolationLevelName" value="ISOLATION_READ_UNCOMMITTED"/>
    <property name="timeout" value="30"/>
</bean>"
You can then inject the sharedTransactionTemplate into as many services as are required.

Finally, instances of the TransactionTemplate class are thread-safe, in that instances do not maintain any conversational state. TransactionTemplate instances do, however, maintain configuration state. So, while a number of classes may share a single instance of a TransactionTemplate, if a class needs to use a TransactionTemplate with different settings (for example, a different isolation level), you need to create two distinct TransactionTemplate instances.


You can register a regular event listener by using the @EventListener annotation. If you need to bind it to the transaction, use @TransactionalEventListener. When you do so, the listener is bound to the commit phase of the transaction by default.

The next example shows this concept. Assume that a component publishes an order-created event and that we want to define a listener that should only handle that event once the transaction in which it has been published has committed successfully. The following example sets up such an event listener:

@Component
public class MyComponent {

    @TransactionalEventListener
    public void handleOrderCreatedEvent(CreationEvent<Order> creationEvent) {
        ...
    }
}
The @TransactionalEventListener annotation exposes a phase attribute that lets you customize the phase of the transaction to which the listener should be bound. The valid phases are BEFORE_COMMIT, AFTER_COMMIT (default), AFTER_ROLLBACK, and AFTER_COMPLETION that aggregates the transaction completion (be it a commit or a rollback).

If no transaction is running, the listener is not invoked at all, since we cannot honor the required semantics. You can, however, override that behavior by setting the fallbackExecution attribute of the annotation to true
