package service;

import model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Persistência JDBC para MySQL/MariaDB. */
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
                            r.getString("contacto"), r.getString("carta_conducao"),
                            TipoVeiculo.valueOf(r.getString("intencao")));
                    clientes.put(x.getId(), x);
                }
            }
            try (ResultSet r = s.executeQuery("SELECT * FROM reservas ORDER BY data_reserva, id")) {
                while (r.next()) {
                    Reserva x = new Reserva(r.getString("id"), clientes.get(r.getString("cliente_id")),
                            veiculos.get(r.getInt("veiculo_id")), r.getTimestamp("data_reserva").toLocalDateTime(),
                            r.getTimestamp("data_expiracao").toLocalDateTime(),
                            EstadoReserva.valueOf(r.getString("estado")));
                    reservas.put(x.getId(), x);
                }
            }
            try (ResultSet r = s.executeQuery("SELECT * FROM contratos ORDER BY data_assinatura, id")) {
                while (r.next()) {
                    LocalDateTime assinatura = r.getTimestamp("data_assinatura").toLocalDateTime();
                    String tipo = r.getString("tipo");
                    Contracto x;
                    if ("ALUGUER".equals(tipo)) {
                        Timestamp fim = r.getTimestamp("data_fim");
                        String periodoGuardado = r.getString("periodo");
                        int mesesAntigos = r.getInt("numero_meses");
                        Integer legado = periodoGuardado == null && !r.wasNull() ? mesesAntigos : null;
                        x = new ContractoAluguer(r.getString("id"), clientes.get(r.getString("cliente_id")),
                                veiculos.get(r.getInt("veiculo_id")), assinatura,
                                r.getString("assinatura_digital_base64"), r.getBytes("imagem_assinatura"),
                                periodoGuardado == null ? null : PeriodoAluguer.valueOf(periodoGuardado), legado,
                                r.getTimestamp("data_inicio") == null ? assinatura : r.getTimestamp("data_inicio").toLocalDateTime(),
                                fim == null ? null : fim.toLocalDateTime(), r.getDouble("mensalidade"));
                    } else {
                        x = new ContractoVenda(r.getString("id"), clientes.get(r.getString("cliente_id")),
                                veiculos.get(r.getInt("veiculo_id")), assinatura,
                                r.getString("assinatura_digital_base64"), r.getBytes("imagem_assinatura"));
                    }
                    contratos.add(x);
                }
            }
        } catch (SQLException e) { throw Database.erro(e); }
    }

    void guardar(Veiculo x) {
        executar("INSERT INTO veiculos(id,marca,modelo,preco,tipo,estado) VALUES(?,?,?,?,?,?)",
                p -> { p.setInt(1,x.getId()); p.setString(2,x.getMarca()); p.setString(3,x.getModelo());
                    p.setDouble(4,x.getPreco()); p.setString(5,x.getTipo().name()); p.setString(6,x.getEstado().name()); });
    }
    void guardar(Cliente x) {
        executar("INSERT INTO clientes(id,nome,email,contacto,carta_conducao,intencao) VALUES(?,?,?,?,?,?)",
                p -> { p.setString(1,x.getId()); p.setString(2,x.getNome()); p.setString(3,x.getEmail());
                    p.setString(4,x.getContacto()); p.setString(5,x.getCartaConducao()); p.setString(6,x.getIntencao().name()); });
    }
    void guardar(Reserva x) {
        executar("INSERT INTO reservas(id,cliente_id,veiculo_id,data_reserva,data_expiracao,estado) VALUES(?,?,?,?,?,?)",
                p -> { p.setString(1,x.getId()); p.setString(2,x.getCliente().getId()); p.setInt(3,x.getVeiculo().getId());
                    p.setTimestamp(4,Timestamp.valueOf(x.getDataReserva())); p.setTimestamp(5,Timestamp.valueOf(x.getDataExpiracao()));
                    p.setString(6,x.getEstado().name()); });
    }
    void guardar(Contracto x, Reserva reserva) {
        executar("INSERT INTO contratos(id,reserva_id,cliente_id,veiculo_id,tipo,data_assinatura,numero_meses,periodo,data_inicio,mensalidade,data_fim,assinatura_digital_base64,imagem_assinatura) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                p -> { p.setString(1,x.getId()); p.setString(2,reserva.getId()); p.setString(3,x.getCliente().getId());
                    p.setInt(4,x.getVeiculo().getId()); p.setString(5,x.getVeiculo().getTipo().name());
                    p.setTimestamp(6,Timestamp.valueOf(x.getDataAssinatura()));
                    if (x instanceof ContractoAluguer) {
                        ContractoAluguer a=(ContractoAluguer)x; p.setNull(7,Types.INTEGER); p.setString(8,a.getPeriodo().name());
                        p.setTimestamp(9,Timestamp.valueOf(a.getDataInicio())); p.setDouble(10,a.getMensalidadeFixa());
                        p.setTimestamp(11,Timestamp.valueOf(a.getDataFim()));
                    } else {
                        p.setNull(7,Types.INTEGER); p.setNull(8,Types.VARCHAR); p.setNull(9,Types.TIMESTAMP);
                        p.setNull(10,Types.DECIMAL); p.setNull(11,Types.TIMESTAMP);
                    }
                    p.setString(12,x.getAssinaturaDigitalBase64());
                    if(x.getImagemAssinatura()==null) p.setNull(13,Types.BLOB); else p.setBytes(13,x.getImagemAssinatura()); });
    }
    void atualizar(Veiculo x) {
        executar("UPDATE veiculos SET estado=? WHERE id=?", p -> {p.setString(1,x.getEstado().name());p.setInt(2,x.getId());});
    }
    void atualizar(Reserva x) {
        executar("UPDATE reservas SET estado=? WHERE id=?", p -> {p.setString(1,x.getEstado().name());p.setString(2,x.getId());});
    }
    int proximoId(String tabela, String coluna, String prefixo) {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING("+coluna+", LENGTH(?) + 1) AS UNSIGNED)),0)+1 FROM "+tabela+" WHERE "+coluna+" LIKE CONCAT(?, '%')";
        try (Connection c=Database.conectar(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,prefixo); p.setString(2,prefixo);
            try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}
        } catch(SQLException e){throw Database.erro(e);}
    }
    int proximoVeiculo() {
        try(Connection c=Database.conectar(); Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT COALESCE(MAX(id),0)+1 FROM veiculos")) {r.next();return r.getInt(1);}
        catch(SQLException e){throw Database.erro(e);}
    }
    private interface Binder { void bind(PreparedStatement p) throws SQLException; }
    private void executar(String sql, Binder binder) {
        try(Connection c=Database.conectar(); PreparedStatement p=c.prepareStatement(sql)) {binder.bind(p);p.executeUpdate();}
        catch(SQLException e){throw Database.erro(e);}
    }
}
