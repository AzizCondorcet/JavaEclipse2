package be.ouagueni.dao;

import java.sql.Connection;

import be.ouagueni.model.Member;

public class MemberDAO extends DAO<Member> {
    public MemberDAO(Connection conn) { super(conn); }

    @Override
    public boolean create(Member obj) {
        // TODO : implémenter INSERT SQL
        return false;
    }

    @Override
    public boolean delete(Member obj) {
        // TODO : implémenter DELETE SQL
        return false;
    }

    @Override
    public boolean update(Member obj) {
        // TODO : implémenter UPDATE SQL
        return false;
    }

    @Override
    public Member find(int id) {
        // TODO : implémenter UPDATE SQL
        return null;
    }
}

