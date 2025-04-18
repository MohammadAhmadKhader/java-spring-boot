package com.example.multitenant.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.example.multitenant.exceptions.AsyncOperationException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

@Component
public abstract class ServicesHelper<TModel> {
    @PersistenceContext
    private EntityManager entityManager; 
    private final Class<TModel> modelClass;

    public ServicesHelper(Class<TModel> modelClass) {
        this.modelClass = modelClass;
    }

    public Page<TModel> findAllWithSpecifications(Pageable pageable, Specification<TModel> spec, BiConsumer<CriteriaQuery<TModel>, Root<TModel>> modifier){
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TModel> query = cb.createQuery(modelClass);
        Root<TModel> root = query.from(modelClass);

        if(modifier != null) {
            modifier.accept(query, root);
        }

        if(spec != null) {
            var predicate = spec.toPredicate(root, query, cb);

            if(predicate != null) {
                query.where(predicate);
            }
        }

        TypedQuery<TModel> typedQuery = entityManager.createQuery(query)
        .setFirstResult((int) pageable.getOffset())
        .setMaxResults(pageable.getPageSize());

        try {
            var listTask = CompletableFuture.supplyAsync(()-> typedQuery.getResultList());
            var countTask = CompletableFuture.supplyAsync(() -> countTotalWithSpec(spec));

            var count = countTask.get();
            var list = listTask.get();
            
            return new PageImpl<TModel>(list, pageable, count);
        }  catch (ExecutionException | InterruptedException e) {
            throw new AsyncOperationException("Error occurred during task execution", e);
        }
    }

    private Long countTotalWithSpec(Specification<TModel> spec){
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(long.class);

        EntityType<TModel> entity = entityManager.getMetamodel().entity(modelClass);
        Root<TModel> root = countQuery.from(entity);


        countQuery.select(cb.countDistinct(root));
        if(spec != null) {
            var predicate = spec.toPredicate(root, countQuery, cb);
            
            if(predicate != null) {
                countQuery.where(predicate);
            }
        }

        return entityManager.createQuery(countQuery).getSingleResult();
    }
}
