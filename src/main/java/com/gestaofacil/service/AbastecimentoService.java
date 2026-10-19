package com.gestaofacil.service;

import com.gestaofacil.controller.form.AbastecimentoForm;
import com.gestaofacil.model.Abastecimento;
import com.gestaofacil.model.StatusVeiculo;
import com.gestaofacil.model.Usuario;
import com.gestaofacil.model.Veiculo;
import com.gestaofacil.repository.AbastecimentoRepository;
import com.gestaofacil.repository.UsuarioRepository;
import com.gestaofacil.repository.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Regras de negocio do abastecimento (card #007).
 *
 * Os mesmos dois padroes do UsoVeiculoService:
 *
 * 1) Motorista e empresa chegam por PARAMETRO, tirados da sessao pelo
 *    controller, e toda busca leva a empresa junto (regra 1).
 *
 * 2) CONFERE TUDO PRIMEIRO, GRAVA DEPOIS. A unica gravacao e a ultima linha.
 *
 * O QUE ESTE SERVICE NAO FAZ, DE PROPOSITO
 * - Nao muda o status do veiculo: nao chama o VeiculoService (regra 2).
 * - Nao compara a quilometragem com a dos abastecimentos anteriores. O card
 *   nao tem essa regra; esta registrado como divergencia no CLAUDE.md.
 */
@Service
public class AbastecimentoService {

    private final AbastecimentoRepository abastecimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VeiculoRepository veiculoRepository;

    public AbastecimentoService(AbastecimentoRepository abastecimentoRepository,
                                UsuarioRepository usuarioRepository,
                                VeiculoRepository veiculoRepository) {
        this.abastecimentoRepository = abastecimentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.veiculoRepository = veiculoRepository;
    }

    /**
     * Registra um abastecimento lancado pelo motorista logado (#007-RF01).
     *
     * Os campos obrigatorios (#007-RN001) ja chegam conferidos pelo @Valid do
     * controller. Mesmo assim, um nulo aqui e recusado pela conferencia de
     * "maior que zero" - o service nao confia que alguem sempre o chame
     * pelo controller.
     *
     * @throws LancamentoRecusadoException quando uma regra recusa o lancamento
     */
    @Transactional
    public Abastecimento registrar(AbastecimentoForm form, Long usuarioId, Long empresaId) {

        Usuario motorista = usuarioRepository.findByIdAndEmpresaId(usuarioId, empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        // #007-RN003 e regra 1: o veiculo precisa ser da empresa do motorista.
        // O controller ja respondeu 404 para id de outra empresa; isto e a
        // mesma conferencia, para o service nao depender dele.
        Veiculo veiculo = veiculoRepository
                .findByIdAndEmpresaId(form.getVeiculoId(), empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Veículo não encontrado na empresa do usuário logado."));

        // #007-RN003: veiculo que saiu da frota nao recebe lancamento novo. A
        // tela nem o oferece; isto cobre o caso de ele ser desativado com a
        // tela ja aberta. EM_USO pode: e o veiculo que esta com o motorista.
        if (veiculo.getStatus() == StatusVeiculo.INATIVO) {
            throw new LancamentoRecusadoException(
                    "O veículo " + veiculo.getNome() + " foi desativado e não recebe"
                            + " novos lançamentos. Escolha outro veículo.");
        }

        // #007-RN006: nada de zero nem negativo.
        if (form.getQuilometragem() == null || form.getQuilometragem() <= 0) {
            throw new LancamentoRecusadoException(
                    "A quilometragem precisa ser maior que zero.");
        }
        exigirMaiorQueZero(form.getLitros(), "Os litros abastecidos precisam ser maiores que zero.");
        exigirMaiorQueZero(form.getValor(), "O valor pago precisa ser maior que zero.");

        // #007-RN005 e RN006: retroativa pode. O que nao pode e um
        // abastecimento que ainda nao aconteceu.
        if (form.getData() == null || form.getData().isAfter(LocalDate.now())) {
            throw new LancamentoRecusadoException(
                    "A data do abastecimento não pode estar no futuro. Confira o que"
                            + " foi informado.");
        }

        // A data de registro nao aparece aqui: quem a preenche e o
        // construtor do Abastecimento, com a hora de agora.
        Abastecimento abastecimento = new Abastecimento(
                motorista.getEmpresa(),
                veiculo,
                motorista,
                form.getData(),
                form.getQuilometragem(),
                form.getLitros(),
                form.getValor(),
                form.getPosto());

        return abastecimentoRepository.save(abastecimento);
    }

    /**
     * Recusa o numero vazio, zero ou negativo.
     *
     * compareTo, e nao equals: para o BigDecimal, 0 e 0.00 nao sao "equals"
     * (a quantidade de casas conta), mas o compareTo diz que sao iguais.
     */
    private void exigirMaiorQueZero(BigDecimal numero, String mensagem) {
        if (numero == null || numero.compareTo(BigDecimal.ZERO) <= 0) {
            throw new LancamentoRecusadoException(mensagem);
        }
    }
}
