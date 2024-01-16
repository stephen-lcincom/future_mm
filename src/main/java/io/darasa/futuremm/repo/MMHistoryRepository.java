package io.darasa.futuremm.repo;


public class MMHistoryRepository extends DwhsRepository<MMHistory> {

  public MMHistoryRepository(AppConfig appConfig, Class<MMHistory> clazz) {
    super(appConfig, clazz);
  }

  public SearchResult<MMHistory> findByMMId(String id, ESPaginate page) {
    return findBy(DwhsHelper.termQuery(ColName.MMHistory.MM_ID, id), page);
  }
}
