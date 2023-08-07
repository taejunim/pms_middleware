package pms.database;

import pms.common.util.ConvertUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;

/**
 * SQL Session
 * <p>
 * - SQL 명령어 처리 및 세션 관리
 */
public class SqlSession {
    private Connection connection;
    private PreparedStatement preparedStatement;

    /**
     * 데이터베이스 조회 실행
     *
     * @param sql SQL 명령어
     * @return ResultSet - 조회 결과
     * @throws SQLException SQLException
     */
    private ResultSet executeQuery(String sql) throws SQLException {
        connection = ConnectionPool.getConnection();
        connection = ConnectionPool.checkConnection(connection);
        preparedStatement = connection.prepareStatement(sql);

        return preparedStatement.executeQuery();    //SQL 명령어 실행 후, 결과 반환
    }

    /**
     * 데이터베이스 등록, 갱신, 삭제 실행
     *
     * @param sql SQL 명령어
     * @return 실행 결과 수
     * @throws SQLException SQLException
     */
    private int executeUpdate(String sql) throws SQLException {
        connection = ConnectionPool.getConnection();
        preparedStatement = connection.prepareStatement(sql);
        /*preparedStatement.executeBatch();
        preparedStatement.clearParameters();
        connection.commit();*/

        return preparedStatement.executeUpdate();   //SQL 명령어 실행 후, 결과 수 반환
    }

    /**
     * 사용 자원 해제
     */
    private void closeResource() {
        try {
            preparedStatement.close();
            ConnectionPool.closeConnection(connection);
        } catch (SQLException e) {
            e.getLocalizedMessage();
        }
    }

    /**
     * 조회 결과 값 설정
     * <p>
     * 조회 결과 값을 해당 Result Class 필드에 지정된 값으로 설정
     *
     * @param resultSet 조회 결과
     * @param type      Result Type
     * @param <T>       Class Type
     * @return Result Class
     * @throws NoSuchMethodException     NoSuchMethodException
     * @throws InvocationTargetException InvocationTargetException
     * @throws InstantiationException    InstantiationException
     * @throws IllegalAccessException    IllegalAccessException
     */
    private <T> T setResult(ResultSet resultSet, Class<T> type) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        T instance = type.getConstructor().newInstance();

        try {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();  //ResultSet 메타 데이터
            int columnCount = resultSetMetaData.getColumnCount();   //컬럼 개수

            //컬럼 개수만큼 지정된 타입 필드에 값 설정
            for (int i = 1; i <= columnCount; i++) {
                String columnName = resultSetMetaData.getColumnLabel(i);    //컬럼 명

                for (Field field : type.getDeclaredFields()) {
                    field.setAccessible(true);

                    String fieldName = ConvertUtil.toUpperSnakeCase(field.getName()); //지정 타입 필드 명

                    //컬럼 명과 필드 명이 일치하는 경우 필드에 값 설정
                    if (columnName.equals(fieldName)) {
                        field.set(instance, resultSet.getObject(i));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return instance;
    }

    /**
     * Select 실행 및 List 형식 결과 반환
     *
     * @param sql        SQL 명령어
     * @param resultType Result Type
     * @param <T>        Class Type
     * @return List 형식의 조회 결과
     */
    public <T> List<T> selectList(String sql, Class<T> resultType) {
        List<T> resultList = new ArrayList<>();

        try {
            ResultSet resultSet = executeQuery(sql);    //조회 쿼리 실행

            while (resultSet.next()) {
                resultList.add(setResult(resultSet, resultType)); //설정된 조회 결과 값 결과 List 추가
            }

            resultSet.close();
        } catch (SQLException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            closeResource();
        }

        return resultList;
    }

    /**
     * Select 실행 및 Map 형식 결과 반환
     *
     * @param sql        SQL 명령어
     * @param resultType Result Type
     * @param mapKey     'Key' 설정할 컬럼
     * @param <T>        Class Type
     * @return Map 형식의 조회 결과
     */
    public <T> Map<Object, T> selectMap(String sql, Class<T> resultType, String mapKey) {
        Map<Object, T> resultMap = new HashMap<>();

        try {
            ResultSet resultSet = executeQuery(sql);    //조회 쿼리 실행

            while (resultSet.next()) {
                Object key = resultSet.getObject(mapKey);  //Key 설정할 해당 컬럼의 값 추출
                resultMap.put(key, setResult(resultSet, resultType)); //추출한 키와 설정된 조회 결과 값을 결과 Map 추가
            }

            resultSet.close();
        } catch (SQLException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            closeResource();
        }

        return resultMap;
    }

    /**
     * Select 실행 및 Map 형식 결과 반환
     * <p>
     * Key 설정한 컬럼이 2개 이상인 경우 사용, 구분자 추가
     *
     * @param sql        SQL 명령어
     * @param resultType Result Type
     * @param mapKeys    'Key' 설정할 컬럼 목록
     * @param separator  Key 설정 시, 구분자
     * @param <T>        Class Type
     * @return Map 형식의 조회 결과
     */
    public <T> Map<Object, T> selectMap(String sql, Class<T> resultType, String[] mapKeys, String separator) {
        Map<Object, T> resultMap = new HashMap<>();

        try {
            ResultSet resultSet = executeQuery(sql);    //조회 쿼리 실행

            while (resultSet.next()) {
                StringBuilder key = new StringBuilder();

                //구분자 추가하여 Map Key 생성
                for (String mapKey : mapKeys) {
                    Object keyValue = resultSet.getObject(mapKey); //Key 설정할 해당 컬럼의 값 추출

                    if (key.toString().isEmpty()) {
                        key.append(keyValue);
                    } else {
                        key.append(separator).append(keyValue); //구분자 추가
                    }
                }

                resultMap.put(String.valueOf(key), setResult(resultSet, resultType)); //생성한 키와 설정된 조회 결과 값을 결과 Map 추가
            }

            resultSet.close();
        } catch (SQLException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            closeResource();
        }

        return resultMap;
    }

    /**
     * Select 실행 및 1건의 지정된 Type 형식 결과 반환
     *
     * @param sql        SQL 명령어
     * @param resultType Result Type
     * @param <T>        Class Type
     * @return 지정된 타입의 조회 결과
     */
    public <T> T selectOne(String sql, Class<T> resultType) {
        T result = null;

        try {
            ResultSet resultSet = executeQuery(sql);    //조회 쿼리 실행

            while (resultSet.next()) {
                result = setResult(resultSet, resultType);    //설정된 조회 결과 값
            }

            resultSet.close();
        } catch (SQLException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            closeResource();
        }

        return result;
    }

    /*public <T> T selectOneTest(String sql, T resultType) {

        T test = null;

        try {
            test = resultType;

            Object object;
            test.getClass().getConstructor().newInstance();

            //instance = resultType.getConstructor().newInstance();
            //instance = resultType.getConstructor().newInstance();
            //instance = resultType.getClass().getConstructor().newInstance();
            ResultSet resultSet = executeQuery(sql);    //조회 쿼리 실행

            while (resultSet.next()) {
                System.out.println(resultSet.getObject(1));

                //instance = (T) resultSet.getObject(1);
                //test = resultSet.getObject(1);

                //test1 = resultSet.getObject(1, test);

            }

            resultSet.close();

        } catch (SQLException  *//*| InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException*//* e) {
            e.printStackTrace();
        } finally {
            closeResource();
        }

        return test;
    }*/

    public int insert(String sql) {
        int result = 0;

        try {
            result = executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResource();
        }

        return result;
    }

    public int update(String sql) {
        int result = 0;

        try {
            result = executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResource();
        }

        return result;
    }

    public int delete(String sql) {
        int result = 0;

        try {
            result = executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResource();
        }

        return result;
    }
}
