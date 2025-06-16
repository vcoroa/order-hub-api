package br.com.vpsconsulting.orderhub.repository;

import br.com.vpsconsulting.orderhub.entity.Parceiro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParceiroRepository extends JpaRepository<Parceiro, Long> {

    // Busca por publicId (para API externa e consultas read-only)
    Optional<Parceiro> findByPublicId(String publicId);

    // Busca por publicId com lock pessimístico (para operações de crédito)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Parceiro p WHERE p.publicId = :publicId")
    Optional<Parceiro> findByPublicIdWithLock(@Param("publicId") String publicId);

    // Verifica se existe por CNPJ (para validação)
    boolean existsByCnpj(String cnpj);

    List<Parceiro> findAllByOrderByDataCriacaoDesc();
}