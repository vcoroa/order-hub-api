package br.com.vpsconsulting.orderhub.repository;

import br.com.vpsconsulting.orderhub.entity.Parceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParceiroRepository extends JpaRepository<Parceiro, Long> {

    // Busca por publicId (para API externa)
    Optional<Parceiro> findByPublicId(String publicId);

    // Verifica se existe por CNPJ (para validação)
    boolean existsByCnpj(String cnpj);

    List<Parceiro> findAllByOrderByDataCriacaoDesc();
}