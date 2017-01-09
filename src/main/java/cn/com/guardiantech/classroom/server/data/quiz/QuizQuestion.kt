package cn.com.guardiantech.classroom.server.data.quiz

import org.jetbrains.annotations.Mutable

/**
 * Created by Codetector on 2017/1/8.
 */
class QuizQuestion (val questionID: Int, var description: String, var type: QuizQuestionType, val options: MutableList<String>, var answer: String, var score: Float){

}