package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * NamedParameterJdbcTemplate
 * SqlParameterSource
 * - BeanPropertySqlParameterSource
 * - MapSqlParameterSource
 * Map
 *
 * BeanPropertyRowMapper
 *
 */
@Slf4j
public class JdbcTemplateItemRepositoryV2 implements ItemRepository {

    private final NamedParameterJdbcTemplate template; //순서 기반이 아닌 이름 기반으로 하게 된다.

    public JdbcTemplateItemRepositoryV2(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Item save(Item item) {
        String sql = "insert into item(item_name, price, quantity) " +
                "values (:itemName, :price, :quantity)";//순서 기반과 다르게 물음표가 들어가지 않는다

        SqlParameterSource param = new BeanPropertySqlParameterSource(item);//이게 이름 기반의 라이브러리

        KeyHolder keyHolder = new GeneratedKeyHolder();
        template.update(sql, param, keyHolder);

        long key = keyHolder.getKey().longValue();
        item.setId(key);
        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item " +
                "set item_name=:itemName, price=:price, quantity=:quantity " +
                "where id=:id";//순서 기반과 다르게 물음표가 들어가지 않는다

        SqlParameterSource param = new MapSqlParameterSource()//이게 이름 기반의 라이브러리
                .addValue("itemName", updateParam.getItemName())
                .addValue("price", updateParam.getPrice())
                .addValue("quantity", updateParam.getQuantity())
                .addValue("id", itemId); //이 부분이 별도로 필요하다.

        template.update(sql, param);
    }

    @Override
    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name, price, quantity from item where id = :id";//순서 기반과 다르게 물음표가 들어가지 않는다
        try {
            Map<String, Object> param = Map.of("id", id);
            Item item = template.queryForObject(sql, param, itemRowMapper());
            return Optional.of(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        SqlParameterSource param = new BeanPropertySqlParameterSource(cond);//이게 이름 기반의 라이브러리

        String sql = "select id, item_name as itemName, price, quantity from item";//as itemName으로 sql 조회 결과 이름을 변경
        //동적 쿼리
        if (StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where";
        }

        boolean andFlag = false;
        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%',:itemName,'%')";
            andFlag = true;
        }

        if (maxPrice != null) {
            if (andFlag) {
                sql += " and";
            }
            sql += " price <= :maxPrice";
        }

        log.info("sql={}", sql);
        return template.query(sql, param, itemRowMapper());
    }

    private RowMapper<Item> itemRowMapper() {
        return BeanPropertyRowMapper.newInstance(Item.class); //camel 변환 지원
//        return ((rs, rowNum) -> {
//            Item item = new Item();
//            item.setId(rs.getLong("id"));
//            item.setItemName(rs.getString("item_name"));
//            item.setPrice(rs.getInt("price"));
//            item.setQuantity(rs.getInt("quantity"));
//            return item;
//        });->이거 대신에 위에 코드를 사용하여 간단하게 표현 가능 -> 스프링에서 제공하는 라이브러리
    }
}
