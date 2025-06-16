// src/main/java/br/com/vpsconsulting/orderhub/repository/PedidoRepository.java
package br.com.vpsconsulting.orderhub.repository;

import br.com.vpsconsulting.orderhub.entity.Pedido;
import br.com.vpsconsulting.orderhub.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByPublicId(String publicId);

    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.itens WHERE p.publicId = :publicId")
    Optional<Pedido> findByPublicIdWithItens(@Param("publicId") String publicId);

    List<Pedido> findAllByOrderByDataCriacaoDesc();

    List<Pedido> findByDataCriacaoBetweenOrderByDataCriacaoDesc(LocalDateTime dataInicio, LocalDateTime dataFim);

    List<Pedido> findByStatusOrderByDataCriacaoDesc(StatusPedido status);
}