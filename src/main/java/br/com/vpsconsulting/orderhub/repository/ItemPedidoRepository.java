package br.com.vpsconsulting.orderhub.repository;

import br.com.vpsconsulting.orderhub.entity.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {
}