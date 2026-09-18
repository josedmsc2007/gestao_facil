package com.gestaofacil.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Onde os arquivos dos anexos ficam guardados (#003-RN013).
 *
 * ESTA CLASSE SO SABE DE DISCO
 * Ela nao conhece regra de negocio, nao conhece o banco e nao decide o que
 * pode ser enviado - quem faz isso e o AnexoUsuarioService. Aqui so entra o
 * "onde" e o "como" gravar, ler e apagar. Separar assim permite testar as
 * regras sem tocar no disco, e trocar o disco por outro lugar (um servidor de
 * arquivos, por exemplo) mexendo num arquivo so.
 *
 * FORA DA AREA PUBLICA - E ISSO QUE A RN013 EXIGE
 * A pasta fica FORA do projeto, definida em gestao-facil.anexos.diretorio.
 * Se os arquivos morassem em src/main/resources/static, o Spring os serviria
 * direto pela URL, sem passar por login nenhum: bastaria alguem descobrir o
 * endereco para baixar a CNH de um funcionario. Como a pasta e de fora, o
 * unico caminho ate o arquivo e o endpoint de download, que confere o login e
 * a empresa antes de devolver qualquer byte.
 *
 * UMA PASTA POR EMPRESA
 * Os arquivos ficam em <diretorio>/<empresaId>/<nome>. Isso mantem o
 * isolamento visivel tambem no disco: dá para abrir a pasta de uma empresa e
 * ver so os documentos dela, o que ajuda em backup e em atendimento.
 */
@Service
public class ArmazenamentoDeAnexos {

    /** A pasta raiz, vinda do application.properties. */
    private final Path diretorioBase;

    public ArmazenamentoDeAnexos(@Value("${gestao-facil.anexos.diretorio}") String diretorio) {
        // normalize() resolve ".." e "." do caminho configurado; toAbsolutePath()
        // evita que a pasta mude de lugar conforme o diretorio de onde a
        // aplicacao foi iniciada.
        this.diretorioBase = Paths.get(diretorio).toAbsolutePath().normalize();
    }

    /**
     * Grava o arquivo enviado e devolve o nome com que ele ficou no disco.
     *
     * O NOME E SORTEADO, NUNCA O ENVIADO
     * UUID.randomUUID() gera um identificador unico. O nome que o navegador
     * mandou nao entra aqui de forma alguma - e por isso que um arquivo
     * chamado "../../../application.properties" nao consegue sair da pasta.
     *
     * @param extensao a extensao ja conferida pelo AnexoUsuarioService
     *                 (".pdf", ".jpg" ou ".png")
     */
    public String gravar(MultipartFile arquivo, Long empresaId, String extensao) {

        String nomeArmazenado = UUID.randomUUID() + extensao;
        Path destino = caminhoDe(empresaId, nomeArmazenado);

        try {
            // Cria <diretorio>/<empresaId> na primeira vez que aquela empresa
            // envia um anexo. Se a pasta ja existir, o metodo nao reclama.
            Files.createDirectories(destino.getParent());

            try (InputStream conteudo = arquivo.getInputStream()) {
                Files.copy(conteudo, destino, StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException erro) {
            // Transformamos em excecao nao verificada para nao obrigar todo
            // metodo da pilha a declarar throws IOException. A mensagem diz o
            // caminho, que e o que ajuda a descobrir permissao de pasta errada.
            throw new UncheckedIOException(
                    "Não foi possível gravar o anexo em " + destino, erro);
        }

        return nomeArmazenado;
    }

    /**
     * O caminho completo de um anexo.
     *
     * A ULTIMA CONFERENCIA ANTES DE TOCAR NO DISCO
     * Mesmo com o nome sendo um UUID gerado por nos, o metodo confere se o
     * caminho resultante continua DENTRO da pasta base. E barato, e protege de
     * um erro futuro: se algum dia alguem passar por aqui um nome vindo da
     * URL, o startsWith barra a saida da pasta.
     */
    public Path caminhoDe(Long empresaId, String nomeArmazenado) {

        Path caminho = diretorioBase
                .resolve(String.valueOf(empresaId))
                .resolve(nomeArmazenado)
                .normalize();

        if (!caminho.startsWith(diretorioBase)) {
            throw new IllegalArgumentException("Caminho de anexo inválido.");
        }

        return caminho;
    }

    /**
     * Apaga o arquivo do disco.
     *
     * deleteIfExists nao reclama quando o arquivo ja nao esta la. E o
     * comportamento certo para este caso: se o arquivo sumiu (apagado a mao,
     * restaurado de um backup antigo), o que importa e que ele nao existe mais
     * - e a tela nao precisa mostrar erro por isso.
     */
    public void apagar(Long empresaId, String nomeArmazenado) {
        try {
            Files.deleteIfExists(caminhoDe(empresaId, nomeArmazenado));
        } catch (IOException erro) {
            throw new UncheckedIOException("Não foi possível remover o arquivo do anexo.", erro);
        }
    }

    /** Usado pelos testes e por mensagens de diagnostico. */
    public Path getDiretorioBase() {
        return diretorioBase;
    }
}
