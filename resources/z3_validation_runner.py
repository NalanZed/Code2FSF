import subprocess
import re
import json
import argparse
import time
from typing import List, Any
from z3 import *
import ast

start_time = 0
# Create the Z3 solver
solver = Solver()

RESOURCE_DIR = "resources"
RUNNABLE_DOR= "resources/runnable"

def get_class_name(java_code: str):
    match = re.search(r'class\s+(\w+)', java_code)
    if match:
        return match.group(1)  # 返回匹配的类名
    else:
        return None  # 如果没有匹配到，返回 None

def run_java_code(java_code: str):
    classname = get_class_name(java_code)
    file_path = RUNNABLE_DOR + "/"  + classname + ".java"
    with open(file_path, "w") as file:
        file.write(java_code)
    try:
        subprocess.run(["javac", file_path], check=True)
    except subprocess.CalledProcessError:
        print("Error during Java compilation.")
        return ""

    try:
        result = subprocess.run(
            ["java", file_path],
            capture_output=True,
            text=True,
        )
        # print(" result.stdout:" + result.stdout)
        return result
    except subprocess.CalledProcessError:
        print("Error during Java execution.")
        return None
def parse_execution_path(execution_output: str) -> List[str]:
    lines = execution_output.splitlines()
    execution_path = []

    for line in lines:
        if "current value" in line or "Entering loop" in line or "Exiting loop" in line or "Evaluating if condition" in line \
                or "Return statement" in line or "Function input" in line or "Entering forloop" in line \
                or "Exiting forloop" in line or ("Under condition" in line and "true" in line):
            execution_path.append(line)


    return execution_path

def combind_expr_and_list(expr: str, exprList: List[str]):
    #默认T和preCts中的Ct都是用()包围起来的
    com_expr = expr
    for ct in exprList:
        com_expr = f"{com_expr} && !({ct})"
    return com_expr.strip().strip("&&")

def simplify_expression(expression):
    """
    Simplify logical expressions and remove redundant negations and redundant conditions.
    """
    # Remove double negatives!! (expr) -> (expr)
    expression = re.sub(r'!\(!\((.*?)\)\)', r'\1', expression)
    expression = re.sub(r'\s+', ' ', expression)  # Remove excess space
    return expression



def replace_variables(current_condition: str, variable: str, new_value: str) -> str:
    """
    Replace the variable in the logical condition with the new value.
    """
    pattern = rf'\b{re.escape(variable)}\b'  # Match variable names exactly
    new_value = f"({new_value})"
    return re.sub(pattern, new_value, current_condition)

class Result:
    def __init__(self, status: int, counter_example: str, path_constrain: str):
        self.status = status
        self.counter_example = counter_example  # string 类型字段
        self.path_constrain = path_constrain
    def to_json(self) -> str:
        """
        将 Result 对象序列化为 JSON 字符串
        """
        return json.dumps(self.__dict__)
    @classmethod
    def from_json(cls, json_string: str) -> 'Result':
        data_dict = json.loads(json_string)
        return cls(data_dict["status"], data_dict["counter_example"], data_dict["path_constrain"])
    def __str__(self):
        return f"Result(status={self.status}, counter_example={self.counter_example}, path_constrain={self.path_constrain})"

class SpecUnit:
    def __init__(self, program: str, T: str, D: str, pre_constrains: List[str]):
        self.program = program  # string 类型字段
        self.T = T
        self.D = D
        self.pre_constrains = pre_constrains

    def to_json(self) -> str:
        """
        将 SpecUnit 对象序列化为 JSON 字符串
        """
        return json.dumps(self.__dict__)

    @classmethod
    def from_json(cls, json_string: str) -> 'SpecUnit':
        """
        从 JSON 字符串反序列化为 SpecUnit 对象
        """
        data_dict = json.loads(json_string)
        return cls(data_dict["program"], data_dict["T"], data_dict["D"],data_dict["pre_constrains"])

    def __str__(self):
        return f"SpecUnit(name={self.program}, T={self.T}, D={self.D},pre_constrains={self.pre_constrains})"

class FSFValidationUnit:
    def __init__(self, allTs: List[str], vars: dict):
        self.allTs = allTs  # string 类型字段
        self.vars = vars
    def to_json(self) -> str:
        """
        将 FSFValidationUnit 对象序列化为 JSON 字符串
        """
        return json.dumps(self.__dict__)

    @classmethod
    def from_json(cls, json_string: str) -> 'FSFValidationUnit':
        """
        从 JSON 字符串反序列化为 FSFValidationUnit 对象
        """
        data_dict = json.loads(json_string)
        return cls(data_dict["allTs"], data_dict["vars"])

    def __str__(self):
        return f"FSFValidationUnit(name={self.allTs}, T={self.vars})"

############# java_expr_z3_expr ##############
def solver_check_z3(z3_expr:str)->str:
    try:
        solver = Solver()
        solver.add(z3_expr)

        if solver.check() == sat:
            print("The expression is satisfiable ❌")
            model = f"{solver.model()}"
            print(model)
            return model
        else:
            print("The expression is unsatisfiable ✅")
            #创建 Result 对象
            return "OK"

    except Exception as e:
        print("solver check fail!")
        print("错误信息:", e)
        return "ERROR"

    except Exception as e:
        print("solver check fail!")
        print("错误信息:", e)
        return "ERROR"
    
def replace_char_literals(expr):
    # 替换 Java 表达式中的字符字面量，如 'a' -> 97
    return re.sub(r"'(.)'", lambda m: str(ord(m.group(1))), expr)

def java_expr_to_z3(expr_str, var_types: dict):
    """
    :param expr_str: Java格式逻辑表达式，如 "(b1 == true && x > 5)"
    :param var_types: dict，变量名到类型的映射，如 {'b1': 'bool', 'x': 'int'}
    :return: Z3 表达式
    """
    expr_str = expr_str.strip()
    expr_str = expr_str.lstrip()  # 进一步去除前导空白
    expr_str = " ".join(expr_str.splitlines())  # 合并为单行，去除多余缩进
    print(f"Java表达式: {repr(expr_str)}")  # 用repr��便调试不可见字符
    # 构建 Z3 变量
    z3_vars = {}
    for name, vtype in var_types.items():
        if vtype == 'boolean' or vtype == 'bool':
            z3_vars[name] = z3.Bool(name)
        elif vtype == 'int':
            z3_vars[name] = z3.Int(name)
        elif vtype == 'char':
            z3_vars[name] = z3.BitVec(name, 16)
        elif vtype == 'double':
            z3_vars[name] = z3.Real(name)
        else:
            raise ValueError(f"不支持的变量类型: {vtype}")

    # 替换 Java 风格语法
    expr_str = replace_char_literals(expr_str)
    expr_str = expr_str.replace("true", "True").replace("false", "False")
    expr_str = expr_str.replace("&&", " and ").replace("||", " or ").replace("!", " not ")
    expr_str = expr_str.replace("not =","!=") # 纠错，由于将! 替换为 not,会导致 != 变为 not =，需要纠正为 !=
    expr_str = expr_str.strip()  # 再次去除前后空白，防止前导空格导致IndentationError

    # AST 转换器
    class Z3Transformer(ast.NodeTransformer):
        def visit_Name(self, node):
            if node.id in z3_vars:
                return z3_vars[node.id]
            elif node.id in {"char","int", "boolean","float", "double"}: #避免 Java 中的类型名被误认为变量
                return ""
            else:
                raise ValueError(f"未知变量: {node.id}")

        def visit_Constant(self, node):
            if isinstance(node.value, (int, bool, str, float)):
                return node.value
            else:
                raise ValueError(f"不支持的常量类型: {node.value}")

        def visit_BoolOp(self, node):
            values = [self.visit(v) for v in node.values]
            if isinstance(node.op, ast.And):
                return z3.And(*values)
            elif isinstance(node.op, ast.Or):
                return z3.Or(*values)
            else:
                raise ValueError(f"不支持的布尔操作: {type(node.op)}")

        def visit_UnaryOp(self, node):
            if isinstance(node.op, ast.Not):
                return z3.Not(self.visit(node.operand))
            if isinstance(node.op, ast.USub):
                return -self.visit(node.operand)
            else:
                raise ValueError(f"不支持的一元操作: {type(node.op)}")

        def visit_Compare(self, node):
            left = self.visit(node.left)
            right = self.visit(node.comparators[0])
            op = node.ops[0]

            if isinstance(op, ast.Eq):
                return left == right
            if isinstance(op,ast.NotEq):
                return left != right
            elif isinstance(op, ast.NotEq):
                return left != right
            elif isinstance(op, ast.Gt):
                return left > right
            elif isinstance(op, ast.GtE):
                return left >= right
            elif isinstance(op, ast.Lt):
                return left < right
            elif isinstance(op, ast.LtE):
                return left <= right
            else:
                raise ValueError(f"不支持的比较运算符: {type(op)}")

        def visit_BinOp(self, node):
            left = self.visit(node.left)
            right = self.visit(node.right)
            op = node.op

            if isinstance(op, ast.Add):
                return left + right
            elif isinstance(op, ast.Sub):
                return left - right
            elif isinstance(op, ast.Mult):
                return left * right
            elif isinstance(op, ast.Div):
                return left / right
            elif isinstance(op, ast.Mod):
                return left % right
            # elif isinstance(op, ast.BitAnd):
            #     return left & right
            else:
                raise ValueError(f"不支持的算术操作: {type(op)}")
    try:
        parsed = ast.parse(expr_str, mode="eval")
    except Exception as e:
        print(f"ast.parse error: {e}, expr_str={repr(expr_str)}")
        z3_expr = f"ERROR Info: {e}"  # 或者根据需要设置默认值
        return z3_expr
    try:
        z3_expr = Z3Transformer().visit(parsed.body)
    except Exception as e:
        print(f"Z3Transformer 处理异常: {e}")
        z3_expr = f"ERROR Info: {e}"  # 或者根据需要设置默认值
    return z3_expr

def parse_md_def(java_code: str) -> dict:
    lines = java_code.splitlines()
    var_types = {}
    for line in lines:
        line = line.strip()
        if line.startswith("public static") and "main" not in line:
            return_type = line.split()[2]
            params_def = line.split("(")[1].split(")")[0]
            var_types["return_value"] = return_type
            if params_def.strip():  # 非空才处理
                params = params_def.split(",")
                for param in params:
                    param = param.strip()
                    param_type = param.split()[0]
                    param_name = param.split()[1]
                    var_types[param_name] = param_type
            print(var_types)
    return var_types
############# java_expr_z3_expr ##############

def generate_test_spec_unit():
    program = read_java_code_from_file(RESOURCE_DIR + "/" + "TestCase.java")
    T = "num < 0"
    D = "result == -num"
    pre_constrains = {}
    su = SpecUnit(program, T, D,pre_constrains)
    return su.to_json()

def deal_with_spec_unit_json(spec_unit_json: str):
    #读取SpecUnit对象
    spec_unit = None
    # print(f"Processing SpecUnit JSON: {spec_unit_json}")
    try:
        spec_unit = SpecUnit.from_json(spec_unit_json)
    except json.JSONDecodeError as e:
        print(f"Error decoding JSON: {e}")
    program = spec_unit.program
    T = spec_unit.T
    D = spec_unit.D
    previous_cts = spec_unit.pre_constrains

    #运行程序,获得输出
    output = run_java_code(program)
    execution_output = ""
    if(output is None):
        print("Java code execution failed.")
        return

    #特殊处理为Exception的TD组
    if "Exception" in D:
        if output.stderr is not None and "Exception" in output.stderr:
            result = Result(0,"","Exception founded!")
            print("result:" + result.to_json())
        else :
            result = Result(1,"","No exception founded!")
            print("result:" + result.to_json())
        return

    if(output.stdout is not None):
        execution_output = output.stdout
    if not execution_output:
        print("No output from Java code execution.")
    #分析路径输出，得到本次执行路径相关的Ct
    execution_path = parse_execution_path(execution_output)
    print("\nExecution Path:")
    for step in execution_path:
        print(step)
    print("end Execution Path")
    current_ct = get_ct_from_execution_path(execution_path);
    if(current_ct == ""):
        current_ct = "true"
    print(f"本次路径对应的_Ct_: {current_ct}")
    new_d = update_D_with_execution_path(D,execution_path)
    print("new_d:" + new_d)
    # 构建新的逻辑表达式并检查可满足性
    negated_d = f"!({new_d})"
    new_logic_expression = f"({T}) && ({current_ct}) && ({negated_d})"
    new_logic_expression = simplify_expression(new_logic_expression)
    print(f"\nT && Ct && !D: {new_logic_expression}")

    var_types = parse_md_def(program)
    z3_expr = java_expr_to_z3(new_logic_expression, var_types)
    # if z3_expr.startswith("ERROR"):
    #     result = Result(1,z3_expr,"")
    #     print("result:" + result.to_json())
    #     return
    print("T && Ct && !D 转Z3表达式: " + str(z3_expr))
    solver_result = solver_check_z3(z3_expr)
    if solver_result == "OK":
        #组装 combined_expr
        previous_cts.append(current_ct)
        combined_expr = combind_expr_and_list(f"({T})", previous_cts)
        print("完成一轮路径验证后，当前(T) && !(previous_cts) && !(current_ct): " + combined_expr)
        z3_expr = java_expr_to_z3(combined_expr, var_types)
        print("(T) && !(previous_cts) && !(current_ct) 转 Z3表达式: " + str(z3_expr))
        scr = solver_check_z3(z3_expr)
        if(scr == "OK"):
            result = Result(3,"",current_ct)
        elif(scr == "ERROR"):
            result = Result(1,scr,"")
        else:
            result = Result(0,"",current_ct)
    elif solver_result == "ERROR":
        result = Result(1,"",current_ct)
    else:
        result = Result(2,solver_result,"")
    print("result:" + result.to_json())

def remove_type_transfer_stmt_in_expr(expr: str) -> str:
    ans = expr.replace("(long)","").replace("(int)","").replace("(short)","").replace("(byte)","").replace("(char)","")
    return ans


def get_ct_from_execution_path(execution_path:List[str]):
    ct = ""
    for step in reversed (execution_path):
        if "Evaluating if condition" in step:
            condition_match = re.search(r"Evaluating if condition: (.*?) is evaluated as: (.*?)", step)
            if condition_match:
                if_condition = condition_match.group(1).strip()
                if_condition = remove_type_transfer_stmt_in_expr(if_condition)
                ct = f"{ct} && ({if_condition})"
            # Check whether it is a condition to enter the loop
        elif "Entering loop" in step:
            condition_match = re.search(r"Entering loop with condition: (.*?) is evaluated as: true", step)
            if condition_match:
                loop_condition = condition_match.group(1).strip()
                ct = f"{ct} && ({loop_condition})"
        elif "Entering forloop" in step:
            condition_match = re.search(r"Entering forloop with condition: (.*?) is evaluated as: true", step)
            if condition_match:
                loop_condition = condition_match.group(1).strip()
                ct = f"{ct} && ({loop_condition})"

            # Check whether it is a condition for exiting the loop
        elif "Exiting loop" in step:
            condition_match = re.search(r"Exiting loop, condition no longer holds: (.*?) is evaluated as: false", step)
            if condition_match:
                loop_condition = condition_match.group(1).strip()
                ct = f"{ct} && !({loop_condition})"
        elif "Exiting forloop" in step:
            condition_match = re.search(r"Exiting forloop, condition no longer holds: (.*?) is evaluated as: false", step)
            if condition_match:
                loop_condition = condition_match.group(1).strip()
                ct = f"{ct} && !({loop_condition})"

            # Check for variable assignment
        elif "current value" in step:
            assignment_match = re.search(r"(.*?) = (.*?), current value of (.*?): (.*?)$", step)
            if assignment_match:
                variable = assignment_match.group(1).strip()
                value = assignment_match.group(2).strip()
                ct = replace_variables(ct,variable,value)
        elif "Under condition" in step:
            condition_assignment_match = re.search(r"Under condition (.*) = (.*), condition is : (.*)", step)
            if condition_assignment_match:
                variable = condition_assignment_match.group(1).strip()
                value = condition_assignment_match.group(2).strip()
                ct = replace_variables(ct,variable,value)

    #先去掉空格，再去掉多余的 &&
    return ct.strip().strip("&&")

def update_D_with_execution_path(D: str, execution_path: List[str]) -> str:
    # split_d = D.split("&&")
    # update_d = []
    print(f"original D : {D}")
    D = D.replace("(char)", "")
    print(f"now D is {D}")
    newd = D
    for step in reversed(execution_path):
        if "current value" in step or "Function input" in step or "Under condition" in step:
            assignment_match = re.search(r"(.*?) = (.*?), current value of (.*?): (.*?)$", step)
            input_param_match = re.search(r"Function input (.*)? parameter (.*?) = (.*?)$", step)
            condition_assignment_match = re.search(r"Under condition (.*) = (.*), condition is : (.*)", step)
            type = ""
            if assignment_match:
                variable = assignment_match.group(1).strip()
                value = assignment_match.group(2).strip()
            elif input_param_match:
                type = input_param_match.group(1).strip()
                variable = input_param_match.group(2).strip()
                value = input_param_match.group(3).strip()
            elif condition_assignment_match:
                variable = condition_assignment_match.group(1).strip()
                value = condition_assignment_match.group(2).strip()
            else :
                continue
            # for sd in split_d:
            #     if variable in sd:
            #         # 替换 D 中的变��
            #         sd = replace_variables(sd, variable, value)
            #     update_d.append(sd.strip())
            # split_d = update_d
            # update_d = []
            if(type and type == "char"):
                value = f"'{value}'" # 给char类型变量带上''
            value = f"({value})" # 确保value不会影响newd结构
            newd = replace_variables(newd,variable,value)
    # for ud in split_d:
    #     newd = f"{newd} && ({ud})"

    return newd.strip().strip("&&")

def read_java_code_from_file(file_path):
    """
    Read Java code from the specified file.
    """
    with open(file_path, "r") as file:
        java_code = file.read()
    return java_code

def fsf_exclusivity_validate(fuJson: str):
    fu = FSFValidationUnit.from_json(fuJson)
    print(fu)
    ts = fu.allTs
    ts_size = len(ts)
    and_ts = []
    or_connect_ts = ""

    #先验证完整性，即!（T1 || T2 || T3 || ...）无解
    for t in ts:
        or_connect_ts = f"{or_connect_ts}||({t})"
    or_connect_ts = or_connect_ts.strip().strip("||").strip()
    or_connect_ts = f"!({or_connect_ts})"
    print("验证完备性: " + or_connect_ts)
    z3_expr = java_expr_to_z3(or_connect_ts, fu.vars)
    if isinstance(z3_expr, str) and z3_expr.startswith("ERROR"):
        result = Result(-1, z3_expr, "")
        print("FSF validation result:" + result.to_json())
        return
    r = solver_check_z3(z3_expr)
    if(r == "ERROR"):
        result = Result(1, "FSF VALIDATION ERROR!", "")
        print("FSF validation result:" + result.to_json())
        return
    if(r == "OK"): #unsat，具有完备性
        print("T具有完备性")
    else: #不具有完备性
        result = Result(3, or_connect_ts, "不具有完备性")
        print("FSF validation result:" + result.to_json())
        return

    #验证排他性，即T1 && T2无解
    for i in range(ts_size):
        for j in range(i + 1, ts_size):
            t1 = ts[i]
            t2 = ts[j]
            and_ts.append(f"({t1}) && ({t2})")
    result = Result(0, "", "")
    for and_t in and_ts:
        print("正在验证: " + and_t)
        z3_expr = java_expr_to_z3(and_t, fu.vars)
        r = solver_check_z3(z3_expr)
        if(r == "OK"):
            continue
        if(r == "ERROR"):
            result = Result(1, "FSF VALIDATION ERROR!", "")
            break
        else:
            result = Result(2, and_t, r)
            break
    print("FSF validation result:" + result.to_json())

def test_main():
    init_files();
    spec_unit_json = generate_test_spec_unit()
    print(spec_unit_json)
    if spec_unit_json is not None:
        deal_with_spec_unit_json(spec_unit_json)

def main():
    #创建解析器
    parser = argparse.ArgumentParser()
    # 添加参数定义
    parser.add_argument('-s', '--specUnit', '--specunit', help='输入要验证的SpecUnit对象的JSON字符串', required=False)
    parser.add_argument('-f', '--fu', '--fsfValidationUnit', help='输入要验证的fsfValidationUnit对象的JSON字符串', required=False)
    # 解析命令行参数
    args = parser.parse_args()
    spec_unit_json = args.specUnit
    fsf_validation_unit_json = args.fu
    if(spec_unit_json is None and fsf_validation_unit_json is None):
        print("请提供输入要验证的JSON字符串")
        return
    if(spec_unit_json is not None):
        deal_with_spec_unit_json(spec_unit_json)
    if(fsf_validation_unit_json is not None):
        fsf_exclusivity_validate(fsf_validation_unit_json)




def init_files():
    import os
    if not os.path.exists(RESOURCE_DIR):
        os.makedirs(RESOURCE_DIR)
    if not os.path.exists(RUNNABLE_DOR):
        os.makedirs(RUNNABLE_DOR)
    if(not os.path.exists(RESOURCE_DIR + "/TestCase.java")):
        program = """
            public class TestCase{
                public static int Abs(int num){
                    if(num < 0){
                        System.out.println("Evaluating if condition: num < 0 is evaluated as: " + (num < 0));
                        return -num;
                    }
                    else{
                        return num;
                    }
                }
            
                public static void main(String[] args){
                    int num = -3;
                    int result = TestCase.Abs(num);
                    System.out.println("result = Abs.Abs(num), current value of result: " + result);
                }
            }
            """
        with open(RESOURCE_DIR + "/TestCase.java", "w") as file:
            file.write(program)
def test_main_4():
    program = read_java_code_from_file("resources/Chufa.java")
    print(program)
    output = run_java_code(program)
    if output is None:
        return "Java code execution failed!"
    if output.stderr is not None and "Exception" in output.stderr:
        print(output.stderr)
        return "Exception founded!"
    else :
        return "No Exception founded!"


if __name__ == "__main__":
    # test_main_2()
    # test_main_3("{\"allTs\":[\"T1\",\"T2\"],\"vars\":{\"a\":\"int\",\"b\":\"String\"}}")
    # test_main()
    main()
    # test_main_4()


