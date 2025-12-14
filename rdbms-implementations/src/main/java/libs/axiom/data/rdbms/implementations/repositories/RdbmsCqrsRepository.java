package libs.axiom.data.rdbms.implementations.repositories;

import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import libs.axiom.data.abstractions.Repository;
import libs.axiom.data.abstractions.utils.AutoCloseableIterator;
import libs.axiom.data.abstractions.utils.TransactionalDetector;
import libs.axiom.data.rdbms.implementations.transaction.RdbmsTransactionManager;
import libs.axiom.messaging.abstractions.ThreadLocalContext;
import libs.axiom.messaging.abstractions.ThreadLocalContextProvider;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Stream;

public class RdbmsCqrsRepository implements Repository {

    private final EntityManagerFactory entityManagerFactory;
    private final ThreadLocalContextProvider threadLocalContextProvider;

    public RdbmsCqrsRepository(EntityManagerFactory entityManagerFactory, ThreadLocalContextProvider threadLocalContextProvider) {
        this.entityManagerFactory = entityManagerFactory;
        this.threadLocalContextProvider = threadLocalContextProvider;
    }

    @Override
    public <T> T get(UUID id, Class<T> type) {

        boolean isTransactional = TransactionalDetector.isTransactionalCall();

        EntityManager entityManager = getEntityManager(isTransactional, true);

        T entity = entityManager.find(type, id);

        if (!isTransactional) {
            entityManager.close();
        }

        return entity;
    }

    @Override
    public <T> T get(Predicate filter, Class<T> type) {

        boolean isTransactional = TransactionalDetector.isTransactionalCall();

        EntityManager entityManager = getEntityManager(isTransactional, true);

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        T entity = queryFactory.selectFrom(getEntityPath(type)).where(filter).fetchFirst();

        if (!isTransactional) {
            entityManager.close();
        }

        return entity;
    }

    @Override
    public <T> List<T> getMany(Predicate filter, Class<T> type) {

        boolean isTransactional = TransactionalDetector.isTransactionalCall();

        EntityManager entityManager = getEntityManager(isTransactional, true);

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        List<T> entities = queryFactory.selectFrom(getEntityPath(type)).where(filter).fetch();

        if (!isTransactional) {
            entityManager.close();
        }

        return entities;
    }

    @Override
    public <T> List<T> getMany(List<UUID> entityIds, Class<T> type) {

        String entityName = type.getSimpleName();

        boolean isTransactional = TransactionalDetector.isTransactionalCall();

        EntityManager entityManager = getEntityManager(isTransactional, true);

        return entityManager.createQuery(
                        "SELECT e FROM " + entityName + " e WHERE e.id IN :idList", type)
                .setParameter("idList", entityIds)
                .getResultList();
    }

    @Override
    public <T> AutoCloseableIterator<T> getIterator(Predicate filter, Class<T> type) {
        EntityManager entityManager = this.entityManagerFactory.createEntityManager();

        entityManager.getTransaction().begin();

        JPAQuery<T> query = createQuery(filter, type, entityManager);

        Stream<T> stream = query.stream();

        final Iterator<T> iterator = stream.iterator();

        return new AutoCloseableIterator<>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                T entity = iterator.next();
                entityManager.detach(entity);
                return entity;
            }

            @Override
            public void close() {
                stream.close();
            }
        };
    }

    @Override
    public <T> AutoCloseableIterator<T> getIterator(Predicate filter, Class<T> type, String collectionName) {
        return getIterator(filter, type);
    }


    @Override
    public <T> boolean exists(Predicate filter, Class<T> type) {

        boolean isTransactional = TransactionalDetector.isTransactionalCall();

        EntityManager entityManager = getEntityManager(isTransactional, true);

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        boolean exists = queryFactory.selectFrom(getEntityPath(type)).where(filter).fetchFirst() != null;

        if (!isTransactional) {
            entityManager.close();
        }

        return exists;
    }

    @Override
    public <T> void save(T entity, Class<T> type) {

        boolean isTransactional = TransactionalDetector.isTransactionalCall();

        EntityManager entityManager = getEntityManager(isTransactional, false);

        entityManager.persist(entity);

        if (!isTransactional) {
            entityManager.getTransaction().commit();
            entityManager.close();
        }
    }

    @Override
    public <T> void save(T entity, Class<T> type, String collectionName) {
        save(entity, type);
    }

    @Override
    public <T> void save(List<T> entities, Class<T> type) {

        boolean isTransactional = TransactionalDetector.isTransactionalCall();

        EntityManager entityManager = getEntityManager(isTransactional, false);

        for (T entity : entities) {
            entityManager.persist(entity);
        }

        if (!isTransactional) {
            entityManager.getTransaction().commit();
            entityManager.close();
        }
    }

    @Override
    public <T> void delete(Predicate filter, Class<T> type) {

        boolean isTransactional = TransactionalDetector.isTransactionalCall();

        EntityManager entityManager = getEntityManager(isTransactional, false);

        new JPADeleteClause(entityManager, getEntityPath(type))
                .where(filter)
                .execute();

        if (!isTransactional) {
            entityManager.getTransaction().commit();
            entityManager.close();
        }
    }

    @Override
    public <T> long update(Predicate filter, T entity, Class<T> type) {

        boolean isTransactional = TransactionalDetector.isTransactionalCall();

        EntityManager entityManager = getEntityManager(isTransactional, false);

        if (!isTransactional) {
            entityManager.getTransaction().commit();
            entityManager.close();
        }

        return 1L;
    }

    @Override
    public <T> long update(T entity, UUID entityId, int version, Class<T> type) {

        boolean isTransactional = TransactionalDetector.isTransactionalCall();

        EntityManager entityManager = getEntityManager(isTransactional, false);

        if (!isTransactional) {
            entityManager.getTransaction().commit();
            entityManager.close();
        }

        return 1L;
    }

    private EntityManager getEntityManager(boolean isTransactional, boolean isReadonly) {

        if (!isTransactional) {

            EntityManager entityManager = this.entityManagerFactory.createEntityManager();

            if (!isReadonly) {
                entityManager.getTransaction().begin();
            }

            return entityManager;
        }

        ThreadLocalContext threadLocalContext = this.threadLocalContextProvider.getThreadLocalContext();

        RdbmsTransactionManager rdbmsTransactionManager = threadLocalContext.getFeature(RdbmsTransactionManager.class);

        if (rdbmsTransactionManager != null) {
            return rdbmsTransactionManager.getEntityManager();
        }

        EntityManager entityManager = this.entityManagerFactory.createEntityManager();

        threadLocalContext.setFeature(new RdbmsTransactionManager(entityManager));

        if (!entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().begin();
        }

        return entityManager;
    }


    @SuppressWarnings("unchecked")
    private <T> EntityPath<T> getEntityPath(Class<T> entityType) {
        String qClassName = "Q" + entityType.getSimpleName();
        String packageName = entityType.getPackage().getName();
        try {
            Class<?> qClass = Class.forName(packageName + "." + qClassName);

            String requiredFieldName = entityType.getSimpleName();

            char first = requiredFieldName.charAt(0);

            requiredFieldName = Character.toLowerCase(first) + requiredFieldName.substring(1);

            Field instanceField = qClass.getDeclaredField(requiredFieldName);
            return (EntityPath<T>) instanceField.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Could not resolve EntityPath for " + entityType, e);
        }
    }

    private <T> JPAQuery<T> createQuery(Predicate filter, Class<T> type, EntityManager session) {
        Path<?> rootEntity = inspectPrimaryRoot(filter);
        assert rootEntity != null;
        EntityPathBase<T> entityPath = new EntityPathBase<>(type, rootEntity.toString());
        JPAQueryFactory queryFactory = new JPAQueryFactory(session);
        return queryFactory.selectFrom(entityPath).where(filter);
    }

    private static Path<?> inspectPrimaryRoot(Expression<?> expression) {
        Stack<Expression<?>> stack = new Stack<>();
        stack.push(expression);
        while (!stack.isEmpty()) {
            Expression<?> current = stack.pop();
            if (current instanceof Path<?> path) {
                return path.getRoot();
            }

            if (current instanceof Operation<?> operation) {
                for (Expression<?> arg : operation.getArgs()) {
                    stack.push(arg);
                }
            }
        }
        return null;
    }
}