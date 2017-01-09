package cn.com.guardiantech.classroom.server.data.quiz

/**
 * Created by Codetector on 2017/1/8.
 */
enum class QuizQuestionType(val value: Int) {
    SingleSelection(1),
    MultipleSelection(1),
    FillBlankWithAutoGrading(1),
    FillBlank(1),
    TextArea(1)
}