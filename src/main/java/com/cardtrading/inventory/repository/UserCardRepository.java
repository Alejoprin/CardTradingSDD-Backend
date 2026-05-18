package com.cardtrading.inventory.repository;

import com.cardtrading.auth.entity.User;
import com.cardtrading.card.entity.Card;
import com.cardtrading.inventory.entity.UserCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCardRepository extends JpaRepository<UserCard, UUID>, JpaSpecificationExecutor<UserCard> {

    Page<UserCard> findByUserId(UUID userId, Pageable pageable);
    List<UserCard> findByUserIdAndCardId(UUID userId, UUID cardId);
    Optional<UserCard> findByIdAndUserId(UUID id, UUID userId);
    Optional<UserCard> findByUserIdAndCardIdAndCondition(UUID userId, UUID cardId, UserCard.CardCondition condition);
    Optional<UserCard> findByUserIdAndCustomCardIdAndCondition(UUID userId, UUID customCardId, UserCard.CardCondition condition);
    Optional<UserCard> findByUserAndCardAndCondition(User user, Card card, UserCard.CardCondition condition);

    @Query("SELECT uc FROM UserCard uc JOIN FETCH uc.user WHERE uc.card.id = :cardId AND uc.user.id != :excludeUserId")
    List<UserCard> findByCardIdExcludingUser(@Param("cardId") UUID cardId, @Param("excludeUserId") UUID excludeUserId);

}
