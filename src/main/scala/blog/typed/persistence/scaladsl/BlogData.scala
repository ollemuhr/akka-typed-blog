package blog.typed.persistence.scaladsl

trait BlogData {
  def postId: String
  def content: PostContent
}
object BlogData {
  val empty: BlogData = EmptyBlog()
}
case class EmptyBlog() extends BlogData {
  override def postId = ???

  override def content = ???
}

final case class BlogWithContent(
  content: PostContent) extends BlogData {

  def postId: String = content.postId
}
final case class PublishedBlog(content: PostContent) extends BlogData {
  override def postId = content.postId
}
final case class PostContent(postId: String, title: String, body: String)

final case class PostSummary(postId: String, title: String)
