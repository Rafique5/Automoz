package service;

import model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Persistência MySQL com operações transacionais nos fluxos de reserva e contrato. */
final class AutomozRepository {
    void carregar(Map<Integer, Veiculo> veiculos, Map<String, Cliente> clientes,
                  Map<String, Reserva> reservas, List<Contracto> contratos) {
        try (Connection c = Database.conectar(); Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("SELECT * FROM veiculos ORDER BY id")) {
                while (r.next()) {
                    Veiculo v = new Veiculo(r.getInt("id"), r.getString("marca"), r.getString("modelo"),
                            r.getDouble("preco"), TipoVeiculo.valueOf(r.getString("tipo")),
                            EstadoVeiculo.valueOf(r.getString("estado")));
                    veiculos.put(v.getId(), v);
                }
            }
            try (ResultSet r = s.executeQuery("SELECT * FROM clientes ORDER BY id")) {
                while (r.next()) {
                    Cliente x = new Cliente(r.getString("id"), r.getString("nome"), r.getString("email"),
                            r.getString("contacto"), r.getString("carta_conducao"));
                    clientes.put(x.getId(), x);
                }
            }
            try (ResultSet r = s.executeQuery("SELECT * FROM reservas ORDER BY data_reserva, id")) {
                while (r.next()) {
                    Timestamp inicio = r.getTimestamp("data_inicio_utilizacao");
                    Timestamp fim = r.getTimestamp("data_fim_utilizacao");
                    String periodo = r.getString("periodo");
                    Reserva x = new Reserva(r.getString("id"), clientes.get(r.getString("cliente_id")),
                            veiculos.get(r.getInt("veiculo_id")), r.getTimestamp("data_reserva").toLocalDateTime(),
                            r.getTimestamp("data_expiracao").toLocalDateTime(), EstadoReserva.valueOf(r.getString("estado")),
                            inicio == null ? null : inicio.toLocalDateTime(), fim == null ? null : fim.toLocalDateTime(),
                            periodo == null ? null : PeriodoAluguer.valueOf(periodo));
                    reservas.put(x.getId(), x);
                }
            }
            try (ResultSet r = s.executeQuery("SELECT * FROM contratos ORDER BY data_assinatura, id")) {
                while (r.next()) {
                    LocalDateTime assinatura = r.getTimestamp("data_assinatura").toLocalDateTime();
                    Contracto x;
                    if ("ALUGUER".equals(r.getString("tipo"))) {
                        Timestamp fim = r.getTimestamp("data_fim");
                        String periodo = r.getString("periodo");
                        int meses = r.getInt("numero_meses");
                        Integer legado = periodo == null && !r.wasNull() ? meses : null;
                        Timestamp inicio = r.getTimestamp("data_inicio");
                        x = new ContractoAluguer(r.getString("id"), clientes.get(r.getString("cliente_id")),
                                veiculos.get(r.getInt("veiculo_id")), assinatura,
                                r.getString("assinatura_digital_base64"), r.getBytes("imagem_assinatura"),
                                periodo == null ? null : PeriodoAluguer.valueOf(periodo), legado,
                                inicio == null ? assinatura : inicio.toLocalDateTime(),
                                fim == null ? null : fim.toLocalDateTime(), r.getDouble("mensalidade"));
                    } else {
                        x = new ContractoVenda(r.getString("id"), clientes.get(r.getString("cliente_id")),
                                veiculos.get(r.getInt("veiculo_id")), assinatura,
                                r.getString("assinatura_digital_base64"), r.getBytes("imagem_assinatura"));
                    }
                    contratos.add(x);
                }
            }
            atualizarEstadosVeiculos(c, veiculos);
        } catch (SQLException e) { throw Database.erro(e); }
    }

    int proximoId(String tabela, String coluna, String prefixo) {
        if (!("clientes".equals(tabela) && "id".equals(coluna) || "reservas".equals(tabela) && "id".equals(coluna) ||
                "contratos".equals(tabela) && "id".equals(coluna))) throw new IllegalArgumentException("Sequencia invalida.");
        return proximaSequencia(tabela, coluna, prefixo);
    }
    int proximoVeiculo() {
        return proximaSequencia("veiculos","id","");
    }

    void prepararSequencias() {
        garantirSequencia("clientes","id","CL");
        garantirSequencia("reservas","id","R");
        garantirSequencia("contratos","id","C");
        garantirSequencia("veiculos","id","");
    }

    private int proximaSequencia(String tabela,String coluna,String prefixo) {
        String entidade=tabela+":"+prefixo;
        garantirSequencia(tabela,coluna,prefixo);
        for(int tentativa=0;tentativa<3;tentativa++)try(Connection c=Database.conectar()) {
            c.setAutoCommit(false);int value;
            try(PreparedStatement p=c.prepareStatement("SELECT proximo FROM automoz_sequencias WHERE entidade=? FOR UPDATE")){
                p.setString(1,entidade);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Falha ao reservar sequencia de identificadores.");value=r.getInt(1);}
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE automoz_sequencias SET proximo=proximo+1 WHERE entidade=?")){p.setString(1,entidade);p.executeUpdate();}
            c.commit();return value;
        } catch(SQLException e){if(e.getErrorCode()==1213&&tentativa<2)continue;throw Database.erro(e);}
        throw new IllegalStateException("Nao foi possivel gerar identificador.");
    }

    private void garantirSequencia(String tabela,String coluna,String prefixo) {
        String entidade=tabela+":"+prefixo;
        try(Connection c=Database.conectar()) {
            try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM automoz_sequencias WHERE entidade=?")){
                p.setString(1,entidade);try(ResultSet r=p.executeQuery()){if(r.next())return;}
            }
            String query="SELECT COALESCE(MAX(CAST(SUBSTRING("+coluna+",LENGTH(?) + 1) AS UNSIGNED)),0)+1 FROM "+tabela+" WHERE "+coluna+" LIKE CONCAT(?, '%')";
            int next;
            try(PreparedStatement p=c.prepareStatement(query)){p.setString(1,prefixo);p.setString(2,prefixo);try(ResultSet r=p.executeQuery()){r.next();next=r.getInt(1);}}
            try(PreparedStatement p=c.prepareStatement("INSERT IGNORE INTO automoz_sequencias(entidade,proximo) VALUES(?,?)")){p.setString(1,entidade);p.setInt(2,next);p.executeUpdate();}
        } catch(SQLException e){throw Database.erro(e);}
    }

    boolean existeCliente(String id) {
        try(Connection c=Database.conectar(); PreparedStatement p=c.prepareStatement("SELECT 1 FROM clientes WHERE id=?")) {
            p.setString(1,id.trim()); try(ResultSet r=p.executeQuery()){return r.next();}
        } catch(SQLException e){throw Database.erro(e);}
    }

    boolean emailRegistado(String email) {
        try(Connection c=Database.conectar(); PreparedStatement p=c.prepareStatement("SELECT 1 FROM clientes WHERE LOWER(email)=LOWER(?)")) {
            p.setString(1,email.trim()); try(ResultSet r=p.executeQuery()){return r.next();}
        } catch(SQLException e){throw Database.erro(e);}
    }

    void guardar(Veiculo x) {
        executar("INSERT INTO veiculos(id,marca,modelo,preco,tipo,estado) VALUES(?,?,?,?,?,?)",
                p -> { p.setInt(1,x.getId()); p.setString(2,x.getMarca()); p.setString(3,x.getModelo());
                    p.setDouble(4,x.getPreco()); p.setString(5,x.getTipo().name()); p.setString(6,x.getEstado().name()); });
    }
    void guardar(Cliente x) {
        new ClienteService().validar(x);
        if(emailRegistado(x.getEmail())) throw new IllegalArgumentException("Ja existe um cliente com este email.");
        try (Connection c=Database.conectar(); PreparedStatement p=c.prepareStatement(
                "INSERT INTO clientes(id,nome,email,contacto,carta_conducao) VALUES(?,?,?,?,?)")) {
            p.setString(1,x.getId()); p.setString(2,x.getNome()); p.setString(3,x.getEmail().trim());
            p.setString(4,x.getContacto()); p.setString(5,x.getCartaConducao()); p.executeUpdate();
        } catch(SQLException e) {
            if (e.getErrorCode()==1062) throw new IllegalArgumentException("Ja existe um cliente com este email.",e);
            throw Database.erro(e);
        }
    }

    /** Regista reserva e altera o estado na mesma transação; SELECT FOR UPDATE serializa instâncias. */
    void criarReserva(Reserva x) {
        try(Connection c=Database.conectar()) {
            c.setAutoCommit(false);
            try {
                TipoVeiculo tipo;
                EstadoVeiculo estado;
                try(PreparedStatement p=c.prepareStatement("SELECT tipo,estado FROM veiculos WHERE id=? FOR UPDATE")) {
                    p.setInt(1,x.getVeiculo().getId());
                    try(ResultSet r=p.executeQuery()) {
                        if(!r.next()) throw new IllegalArgumentException("Veiculo nao encontrado.");
                        tipo=TipoVeiculo.valueOf(r.getString(1)); estado=EstadoVeiculo.valueOf(r.getString(2));
                    }
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE reservas SET estado='EXPIRADA' WHERE veiculo_id=? AND estado='ACTIVA' AND data_expiracao<=NOW()")) {p.setInt(1,x.getVeiculo().getId());p.executeUpdate();}
                estado=estadoVeiculo(c,x.getVeiculo().getId(),tipo);
                if(tipo==TipoVeiculo.VENDA) {
                    if(estado!=EstadoVeiculo.DISPONIVEL) throw new IllegalStateException("Veiculo de venda ja reservado ou vendido.");
                } else {
                    if(estado==EstadoVeiculo.VENDIDO) throw new IllegalStateException("Veiculo ja vendido.");
                    String overlap="SELECT COUNT(*) FROM reservas WHERE veiculo_id=? AND " +
                            "((estado='ACTIVA' AND data_expiracao>NOW()) OR estado='CONFIRMADA') " +
                            "AND data_inicio_utilizacao < ? AND data_fim_utilizacao > ?";
                    try(PreparedStatement p=c.prepareStatement(overlap)) {
                        p.setInt(1,x.getVeiculo().getId()); p.setTimestamp(2,Timestamp.valueOf(x.getDataFimUtilizacao()));
                        p.setTimestamp(3,Timestamp.valueOf(x.getDataInicioUtilizacao()));
                        try(ResultSet r=p.executeQuery()){r.next();if(r.getInt(1)>0)throw new IllegalStateException("O veiculo ja tem uma reserva nesse intervalo.");}
                    }
                }
                try(PreparedStatement p=c.prepareStatement("INSERT INTO reservas(id,cliente_id,veiculo_id,data_reserva,data_expiracao,data_inicio_utilizacao,data_fim_utilizacao,periodo,estado) VALUES(?,?,?,?,?,?,?,?,?)")) {
                    p.setString(1,x.getId());p.setString(2,x.getCliente().getId());p.setInt(3,x.getVeiculo().getId());
                    p.setTimestamp(4,Timestamp.valueOf(x.getDataReserva()));p.setTimestamp(5,Timestamp.valueOf(x.getDataExpiracao()));
                    nullableTimestamp(p,6,x.getDataInicioUtilizacao());nullableTimestamp(p,7,x.getDataFimUtilizacao());
                    if(x.getPeriodo()==null)p.setNull(8,Types.VARCHAR);else p.setString(8,x.getPeriodo().name());
                    p.setString(9,x.getEstado().name());p.executeUpdate();
                }
                EstadoVeiculo novo=tipo==TipoVeiculo.VENDA || intervaloIncluiAgora(x.getDataInicioUtilizacao(),x.getDataFimUtilizacao())
                        ? EstadoVeiculo.RESERVADO : estado;
                try(PreparedStatement p=c.prepareStatement("UPDATE veiculos SET estado=? WHERE id=?")){p.setString(1,novo.name());p.setInt(2,x.getVeiculo().getId());p.executeUpdate();}
                c.commit(); x.getVeiculo().setEstado(novo);
            } catch(SQLException e){rollback(c);throw e;} catch(RuntimeException e){rollback(c);throw e;}
        } catch(SQLException e){throw Database.erro(e);}
    }

    /** Insere o contrato e confirma reserva/veículo atomicamente. */
    void guardarContrato(Contracto x, Reserva reserva) {
        try(Connection c=Database.conectar()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement p=c.prepareStatement("SELECT estado FROM veiculos WHERE id=? FOR UPDATE")) {
                    p.setInt(1,x.getVeiculo().getId());try(ResultSet r=p.executeQuery()) {
                        if(!r.next())throw new IllegalArgumentException("Veiculo nao encontrado.");
                        if(EstadoVeiculo.VENDIDO.name().equals(r.getString(1)) || (x.getVeiculo().getTipo()==TipoVeiculo.VENDA && !EstadoVeiculo.RESERVADO.name().equals(r.getString(1))))
                            throw new IllegalStateException("Veiculo ja nao esta disponivel.");
                    }
                }
                try(PreparedStatement p=c.prepareStatement("SELECT estado,data_expiracao FROM reservas WHERE id=? FOR UPDATE")) {
                    p.setString(1,reserva.getId());try(ResultSet r=p.executeQuery()) {
                        if(!r.next()||!"ACTIVA".equals(r.getString(1)))throw new IllegalStateException("A reserva ja nao esta activa.");
                        if(!r.getTimestamp(2).toLocalDateTime().isAfter(LocalDateTime.now()))throw new IllegalStateException("A reserva expirou.");
                    }
                }
                inserirContrato(c,x,reserva);
                EstadoVeiculo estado;
                if(x instanceof ContractoVenda) estado=EstadoVeiculo.VENDIDO;
                else {
                    ContractoAluguer a=(ContractoAluguer)x;
                    estado=intervaloIncluiAgora(a.getDataInicio(),a.getDataFim())?EstadoVeiculo.ALUGADO:EstadoVeiculo.RESERVADO;
                }
                try(PreparedStatement p=c.prepareStatement("UPDATE reservas SET estado='CONFIRMADA' WHERE id=?")){p.setString(1,reserva.getId());p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("UPDATE veiculos SET estado=? WHERE id=?")){p.setString(1,estado.name());p.setInt(2,x.getVeiculo().getId());p.executeUpdate();}
                c.commit(); reserva.setEstado(EstadoReserva.CONFIRMADA);x.getVeiculo().setEstado(estado);
            } catch(SQLException e){rollback(c);throw e;} catch(RuntimeException e){rollback(c);throw e;}
        } catch(SQLException e){throw Database.erro(e);}
    }

    private void inserirContrato(Connection c, Contracto x, Reserva reserva) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("INSERT INTO contratos(id,reserva_id,cliente_id,veiculo_id,tipo,data_assinatura,numero_meses,periodo,data_inicio,mensalidade,data_fim,assinatura_digital_base64,imagem_assinatura) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            p.setString(1,x.getId());p.setString(2,reserva.getId());p.setString(3,x.getCliente().getId());p.setInt(4,x.getVeiculo().getId());
            p.setString(5,x.getVeiculo().getTipo().name());p.setTimestamp(6,Timestamp.valueOf(x.getDataAssinatura()));
            if(x instanceof ContractoAluguer) {
                ContractoAluguer a=(ContractoAluguer)x;p.setNull(7,Types.INTEGER);p.setString(8,a.getPeriodo().name());
                p.setTimestamp(9,Timestamp.valueOf(a.getDataInicio()));p.setDouble(10,a.getMensalidadeFixa());p.setTimestamp(11,Timestamp.valueOf(a.getDataFim()));
            } else {p.setNull(7,Types.INTEGER);p.setNull(8,Types.VARCHAR);p.setNull(9,Types.TIMESTAMP);p.setNull(10,Types.DECIMAL);p.setNull(11,Types.TIMESTAMP);}
            p.setString(12,x.getAssinaturaDigitalBase64());if(x.getImagemAssinatura()==null)p.setNull(13,Types.BLOB);else p.setBytes(13,x.getImagemAssinatura());p.executeUpdate();
        }
    }

    void atualizarReserva(Reserva r) { executar("UPDATE reservas SET estado=? WHERE id=?",p->{p.setString(1,r.getEstado().name());p.setString(2,r.getId());}); }
    void cancelarReserva(Reserva r) {
        try(Connection c=Database.conectar()) {
            c.setAutoCommit(false);
            try(PreparedStatement p=c.prepareStatement("SELECT id FROM veiculos WHERE id=? FOR UPDATE")){p.setInt(1,r.getVeiculo().getId());p.executeQuery().close();}
            try(PreparedStatement p=c.prepareStatement("UPDATE reservas SET estado='CANCELADA' WHERE id=? AND estado='ACTIVA'")){p.setString(1,r.getId());if(p.executeUpdate()!=1)throw new IllegalStateException("A reserva ja nao esta activa.");}
            EstadoVeiculo estado=estadoVeiculo(c,r.getVeiculo().getId(),r.getVeiculo().getTipo());
            try(PreparedStatement p=c.prepareStatement("UPDATE veiculos SET estado=? WHERE id=?")){p.setString(1,estado.name());p.setInt(2,r.getVeiculo().getId());p.executeUpdate();}
            c.commit();r.setEstado(EstadoReserva.CANCELADA);r.getVeiculo().setEstado(estado);
        } catch(SQLException e){throw Database.erro(e);}
    }

    Map<Integer,EstadoVeiculo> atualizarEstadosVeiculos() {
        try(Connection c=Database.conectar()) {
            Map<Integer,EstadoVeiculo> estados=new HashMap<Integer,EstadoVeiculo>();
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT id,tipo FROM veiculos")) {
                while(r.next()) {
                    int id=r.getInt(1);TipoVeiculo tipo=TipoVeiculo.valueOf(r.getString(2));
                    EstadoVeiculo estado=estadoVeiculo(c,id,tipo);
                    try(PreparedStatement p=c.prepareStatement("UPDATE veiculos SET estado=? WHERE id=? AND estado<>'VENDIDO'")){p.setString(1,estado.name());p.setInt(2,id);p.executeUpdate();}
                    estados.put(id,estado);
                }
            }
            return estados;
        } catch(SQLException e){throw Database.erro(e);}
    }

    private void atualizarEstadosVeiculos(Connection c,Map<Integer,Veiculo> vs)throws SQLException {
        for(Veiculo v:vs.values()) if(v.getEstado()!=EstadoVeiculo.VENDIDO) v.setEstado(estadoVeiculo(c,v.getId(),v.getTipo()));
    }

    private EstadoVeiculo estadoVeiculo(Connection c,int id,TipoVeiculo tipo)throws SQLException {
        if(tipo==TipoVeiculo.VENDA) {
            try(PreparedStatement p=c.prepareStatement("SELECT estado FROM veiculos WHERE id=?")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(r.next()&&"VENDIDO".equals(r.getString(1)))return EstadoVeiculo.VENDIDO;}}
        } else {
            String sql="SELECT COUNT(*) FROM contratos c JOIN reservas r ON r.id=c.reserva_id WHERE c.veiculo_id=? AND c.tipo='ALUGUER' AND r.estado='CONFIRMADA' AND c.data_inicio<=NOW() AND c.data_fim>NOW()";
            try(PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,id);try(ResultSet r=p.executeQuery()){r.next();if(r.getInt(1)>0)return EstadoVeiculo.ALUGADO;}}
        }
        String sql="SELECT COUNT(*) FROM reservas WHERE veiculo_id=? AND ((estado='ACTIVA' AND data_expiracao>NOW()) OR (estado='CONFIRMADA' AND data_fim_utilizacao>NOW()))";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,id);try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1)>0?EstadoVeiculo.RESERVADO:EstadoVeiculo.DISPONIVEL;}}
    }

    boolean autenticarFuncionario(String usuario,String senha) {
        try(Connection c=Database.conectar();PreparedStatement p=c.prepareStatement("SELECT sal_base64,hash_senha_base64 FROM funcionarios WHERE utilizador=?")) {
            p.setString(1,usuario.trim());try(ResultSet r=p.executeQuery()){return r.next()&&PasswordHasher.verificar(senha,r.getString(1),r.getString(2));}
        } catch(SQLException e){throw Database.erro(e);}
    }

    void alterarSenhaFuncionario(String usuario,String atual,String nova) {
        try(Connection c=Database.conectar()) {
            c.setAutoCommit(false);String salt,hash;
            try(PreparedStatement p=c.prepareStatement("SELECT sal_base64,hash_senha_base64 FROM funcionarios WHERE utilizador=? FOR UPDATE")) {
                p.setString(1,usuario.trim());try(ResultSet r=p.executeQuery()) {
                    if(!r.next()||!PasswordHasher.verificar(atual,r.getString(1),r.getString(2)))
                        throw new IllegalArgumentException("Palavra-passe atual incorreta.");
                }
            }
            salt=PasswordHasher.novoSalt();hash=PasswordHasher.hash(nova,salt);
            try(PreparedStatement p=c.prepareStatement("UPDATE funcionarios SET sal_base64=?,hash_senha_base64=? WHERE utilizador=?")) {
                p.setString(1,salt);p.setString(2,hash);p.setString(3,usuario.trim());p.executeUpdate();
            }
            c.commit();
        } catch(SQLException e){throw Database.erro(e);}
    }

    void criarFuncionarioInicial(String usuario,String senha) {
        try(Connection c=Database.conectar();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM funcionarios")) {
            r.next();if(r.getInt(1)>0)return;
        } catch(SQLException e){throw Database.erro(e);}
        String salt=PasswordHasher.novoSalt(), hash=PasswordHasher.hash(senha,salt);
        executar("INSERT IGNORE INTO funcionarios(utilizador,sal_base64,hash_senha_base64) VALUES(?,?,?)",p->{p.setString(1,usuario);p.setString(2,salt);p.setString(3,hash);});
    }

    boolean devolverVeiculo(int idVeiculo,List<Contracto> contratos,List<Reserva> reservas) {
        ContractoAluguer ativo=null;LocalDateTime agora=LocalDateTime.now();
        for(Contracto c:contratos) if(c instanceof ContractoAluguer) {
            ContractoAluguer a=(ContractoAluguer)c;
            if(a.getVeiculo().getId()==idVeiculo&&a.getDataInicio()!=null&&a.getDataFim()!=null&&
                    !agora.isBefore(a.getDataInicio())&&agora.isBefore(a.getDataFim())) ativo=a;
        }
        if(ativo==null)throw new IllegalStateException("Nao existe aluguer em curso para este veiculo.");
        LocalDateTime fimAnterior=ativo.getDataFim();ativo.setDataFim(agora);
        try(Connection c=Database.conectar()) {
            c.setAutoCommit(false);
            try(PreparedStatement p=c.prepareStatement("SELECT id FROM veiculos WHERE id=? FOR UPDATE")){p.setInt(1,idVeiculo);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Veiculo nao encontrado.");}}
            try(PreparedStatement p=c.prepareStatement("SELECT id FROM contratos WHERE id=? AND data_fim>NOW() FOR UPDATE")){p.setString(1,ativo.getId());try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalStateException("O aluguer ja terminou ou foi devolvido.");}}
            try(PreparedStatement p=c.prepareStatement("UPDATE contratos SET data_fim=? WHERE id=?")){p.setTimestamp(1,Timestamp.valueOf(agora));p.setString(2,ativo.getId());p.executeUpdate();}
            try(PreparedStatement p=c.prepareStatement("UPDATE reservas r JOIN contratos ct ON ct.reserva_id=r.id SET r.data_fim_utilizacao=? WHERE ct.id=?")){p.setTimestamp(1,Timestamp.valueOf(agora));p.setString(2,ativo.getId());p.executeUpdate();}
            EstadoVeiculo estado=estadoVeiculo(c,idVeiculo,TipoVeiculo.ALUGUER);
            try(PreparedStatement p=c.prepareStatement("UPDATE veiculos SET estado=? WHERE id=?")){p.setString(1,estado.name());p.setInt(2,idVeiculo);p.executeUpdate();}
            c.commit();ativo.getVeiculo().setEstado(estado);
            for(Reserva r:reservas)if(r.getVeiculo().getId()==idVeiculo&&r.getEstado()==EstadoReserva.CONFIRMADA&&
                    r.getDataInicioUtilizacao()!=null&&r.getDataFimUtilizacao()!=null&&
                    !agora.isBefore(r.getDataInicioUtilizacao())&&agora.isBefore(r.getDataFimUtilizacao()))r.setDataFimUtilizacao(agora);
            return true;
        } catch(SQLException e){ativo.setDataFim(fimAnterior);throw Database.erro(e);}
        catch(RuntimeException e){ativo.setDataFim(fimAnterior);throw e;}
    }

    private static boolean intervaloIncluiAgora(LocalDateTime inicio,LocalDateTime fim){LocalDateTime n=LocalDateTime.now();return inicio!=null&&fim!=null&&!n.isBefore(inicio)&&n.isBefore(fim);}
    private static void nullableTimestamp(PreparedStatement p,int i,LocalDateTime v)throws SQLException{if(v==null)p.setNull(i,Types.TIMESTAMP);else p.setTimestamp(i,Timestamp.valueOf(v));}
    private interface Binder{void bind(PreparedStatement p)throws SQLException;}
    private void executar(String sql,Binder b){try(Connection c=Database.conectar();PreparedStatement p=c.prepareStatement(sql)){b.bind(p);p.executeUpdate();}catch(SQLException e){throw Database.erro(e);}}
    private void rollback(Connection c){try{c.rollback();}catch(SQLException ignored){}}
}
