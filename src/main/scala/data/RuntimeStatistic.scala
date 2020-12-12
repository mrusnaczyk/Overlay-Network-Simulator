package data

class RuntimeStatistic (val category: String, val data: Int, val unit: String) {
  override def toString: String =
    s"RuntimeStatistic (category = $category, data = $data, unit = $unit)"
}
